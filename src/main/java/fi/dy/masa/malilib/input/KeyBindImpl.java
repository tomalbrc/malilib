package fi.dy.masa.malilib.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.MinecraftClientAccessor;
import fi.dy.masa.malilib.config.value.HudAlignment;
import fi.dy.masa.malilib.config.value.KeybindDisplayMode;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.ToastWidget;
import fi.dy.masa.malilib.input.KeyBindSettings.Context;
import fi.dy.masa.malilib.input.callback.HotkeyCallback;
import fi.dy.masa.malilib.render.message.MessageUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class KeyBindImpl implements KeyBind
{
    private static final List<Integer> PRESSED_KEYS = new ArrayList<>();
    private static int triggeredCount;

    private final KeyBindSettings defaultSettings;
    private final ImmutableList<Integer> defaultKeyCodes;
    private ImmutableList<Integer> keyCodes = ImmutableList.of();
    private ImmutableList<Integer> lastSavedKeyCodes;
    private KeyBindSettings settings;
    private KeyBindSettings lastSavedSettings;
    private String modName = "";
    private String nameTranslationKey = "";
    private boolean pressed;
    private boolean pressedLast;
    private int heldTime;
    @Nullable
    private HotkeyCallback callback;

    private KeyBindImpl(String defaultStorageString, KeyBindSettings settings)
    {
        this.defaultSettings = settings;
        this.defaultKeyCodes = readKeysFromStorageString(defaultStorageString);
        this.settings = settings;

        this.cacheSavedValue();
    }

    @Override
    public void setModName(String modName)
    {
        this.modName = modName;
    }

    @Override
    public void setNameTranslationKey(String nameTranslationKey)
    {
        this.nameTranslationKey = nameTranslationKey;
    }

    @Override
    public KeyBindSettings getSettings()
    {
        return this.settings;
    }

    @Override
    public KeyBindSettings getDefaultSettings()
    {
        return this.defaultSettings;
    }

    @Override
    public void setSettings(KeyBindSettings settings)
    {
        this.settings = settings;
    }

    @Override
    public void setCallback(@Nullable HotkeyCallback callback)
    {
        this.callback = callback;
    }

    @Override
    public boolean isValid()
    {
        return this.keyCodes.isEmpty() == false || this.settings.getAllowEmpty();
    }

    /**
     * Checks if this keybind is now active but previously was not active,
     * and then updates the cached state.
     * @return true if this keybind just became pressed
     */
    @Override
    public boolean isPressed()
    {
        return this.pressed && this.pressedLast == false && this.heldTime == 0;
    }

    @Override
    public boolean isKeyBindHeld()
    {
        return this.pressed || (this.settings.getAllowEmpty() && this.keyCodes.isEmpty());
    }

    /**
     * NOT PUBLIC API - DO NOT CALL FROM MOD CODE!!!
     */
    @Override
    public boolean updateIsPressed()
    {
        if (this.keyCodes.isEmpty() ||
            (this.settings.getContext() != KeyBindSettings.Context.ANY &&
            ((this.settings.getContext() == KeyBindSettings.Context.INGAME) != (GuiUtils.getCurrentScreen() == null))))
        {
            this.pressed = false;
            return false;
        }

        boolean allowExtraKeys = this.settings.getAllowExtraKeys();
        boolean allowOutOfOrder = this.settings.isOrderSensitive() == false;
        boolean pressedLast = this.pressed;
        final int sizePressed = PRESSED_KEYS.size();
        final int sizeRequired = this.keyCodes.size();

        if (sizePressed >= sizeRequired && (allowExtraKeys || sizePressed == sizeRequired))
        {
            int keyCodeIndex = 0;
            this.pressed = PRESSED_KEYS.containsAll(this.keyCodes);

            for (Integer keyCodeObj : PRESSED_KEYS)
            {
                if (this.keyCodes.get(keyCodeIndex).equals(keyCodeObj))
                {
                    // Fully matched keybind
                    if (++keyCodeIndex >= sizeRequired)
                    {
                        break;
                    }
                }
                else if ((allowOutOfOrder == false && (keyCodeIndex > 0 || sizePressed == sizeRequired)) ||
                                 (this.keyCodes.contains(keyCodeObj) == false && allowExtraKeys == false))
                {
                    /*
                    System.out.printf("km fail: key: %s, ae: %s, aoo: %s, cont: %s, keys: %s, pressed: %s, triggeredCount: %d\n",
                            keyCodeObj, allowExtraKeys, allowOutOfOrder, this.keyCodes.contains(keyCodeObj), this.keyCodes, pressedKeys, triggeredCount);
                    */
                    this.pressed = false;
                    break;
                }
            }
        }
        else
        {
            this.pressed = false;
        }

        KeyAction activateOn = this.settings.getActivateOn();

        if (this.pressed != pressedLast &&
            (triggeredCount == 0 || this.settings.isExclusive() == false) &&
            (activateOn == KeyAction.BOTH || this.pressed == (activateOn == KeyAction.PRESS)))
        {
            boolean cancel = this.triggerKeyAction(pressedLast);
            //System.out.printf("triggered, cancel: %s, triggeredCount: %d\n", cancel, triggeredCount);

            if (cancel)
            {
                ++triggeredCount;
            }

            return cancel;
        }

        return false;
    }

    private boolean triggerKeyAction(boolean pressedLast)
    {
        if (this.pressed == false)
        {
            this.heldTime = 0;
            KeyAction activateOn = this.settings.getActivateOn();

            if (pressedLast && (activateOn == KeyAction.RELEASE || activateOn == KeyAction.BOTH))
            {
                return this.triggerKeyCallback(KeyAction.RELEASE);
            }
        }
        else if (pressedLast == false && this.heldTime == 0)
        {
            if (this.keyCodes.contains(Keyboard.KEY_F3))
            {
                // Prevent the debug GUI from opening after the F3 key is released
                ((MinecraftClientAccessor) Minecraft.getMinecraft()).setActionKeyF3(true);
            }

            KeyAction activateOn = this.settings.getActivateOn();

            if (activateOn == KeyAction.PRESS || activateOn == KeyAction.BOTH)
            {
                return this.triggerKeyCallback(KeyAction.PRESS);
            }
        }

        return false;
    }

    private boolean triggerKeyCallback(KeyAction action)
    {
        boolean cancel;

        if (this.callback == null)
        {
            cancel = action == KeyAction.PRESS && this.settings.shouldCancel();
            this.addToastMessage(false, cancel);
        }
        else
        {
            cancel = this.callback.onKeyAction(action, this) && this.settings.shouldCancel();
            this.addToastMessage(true, cancel);
        }

        return cancel;
    }

    private void addToastMessage(boolean hasCallback, boolean cancelled)
    {
        KeybindDisplayMode val = MaLiLibConfigs.Generic.KEYBIND_DISPLAY.getOptionListValue();
        boolean showCallbackOnly = MaLiLibConfigs.Generic.KEYBIND_DISPLAY_CALLBACK_ONLY.getBooleanValue();
        boolean showCancelledOnly = MaLiLibConfigs.Generic.KEYBIND_DISPLAY_CANCEL_ONLY.getBooleanValue();

        if (val != KeybindDisplayMode.NONE &&
            (showCancelledOnly == false || cancelled) &&
            (showCallbackOnly == false || hasCallback))
        {
            List<String> lines = new ArrayList<>();

            if (val == KeybindDisplayMode.KEYS || val == KeybindDisplayMode.KEYS_ACTIONS)
            {
                lines.add(StringUtils.translate("malilib.toast.keybind_display.keys", this.getKeysDisplayString()));
            }

            if (val == KeybindDisplayMode.ACTIONS || val == KeybindDisplayMode.KEYS_ACTIONS)
            {
                String name = StringUtils.translate(this.nameTranslationKey);
                lines.add(StringUtils.translate("malilib.toast.keybind_display.action", this.modName, name));
            }

            HudAlignment align = MaLiLibConfigs.Generic.KEYBIND_DISPLAY_ALIGNMENT.getOptionListValue();
            ToastWidget.updateOrAddToast(align, lines, MaLiLibConfigs.Generic.KEYBIND_DISPLAY_DURATION.getIntegerValue());
        }
    }

    @Override
    public void clearKeys()
    {
        this.keyCodes = ImmutableList.of();
        this.pressed = false;
        this.heldTime = 0;
    }

    @Override
    public void setKeys(List<Integer> newKeys)
    {
        this.keyCodes = ImmutableList.copyOf(newKeys);
    }

    @Override
    public void tick()
    {
        if (this.pressed)
        {
            this.heldTime++;
        }

        this.pressedLast = this.pressed;
    }

    @Override
    public ImmutableList<Integer> getKeys()
    {
        return this.keyCodes;
    }

    @Override
    public ImmutableList<Integer> getDefaultKeys()
    {
        return this.defaultKeyCodes;
    }

    @Override
    public String getKeysDisplayString()
    {
        return writeKeysToString(this.keyCodes, " + ");
    }

    /**
     * Returns true if the keybind has been changed from the default value
     */
    @Override
    public boolean isModified()
    {
        return this.keyCodes.equals(this.defaultKeyCodes) == false;
    }

    @Override
    public boolean isDirty()
    {
        return this.lastSavedKeyCodes.equals(this.keyCodes) == false ||
               this.lastSavedSettings.equals(this.settings) == false;
    }

    @Override
    public void cacheSavedValue()
    {
        this.lastSavedKeyCodes = this.keyCodes;
        this.lastSavedSettings = this.settings;
    }

    @Override
    public void resetToDefault()
    {
        this.keyCodes = this.defaultKeyCodes;
    }

    @Override
    public boolean areSettingsModified()
    {
        return this.settings.equals(this.defaultSettings) == false;
    }

    @Override
    public void resetSettingsToDefaults()
    {
        this.settings = this.defaultSettings;
    }

    @Override
    public void setValueFromString(String str)
    {
        this.clearKeys();
        this.keyCodes = readKeysFromStorageString(str);
    }

    @Override
    public boolean matches(int keyCode)
    {
        return this.keyCodes.size() == 1 && this.keyCodes.get(0) == keyCode;
    }

    public static boolean hotkeyMatchesKeyBind(Hotkey hotkey, KeyBinding keybind)
    {
        return hotkey.getKeyBind().matches(keybind.getKeyCode());
    }

    @Override
    public boolean overlaps(KeyBind other)
    {
        if (other == this || other.getKeys().size() > this.getKeys().size())
        {
            return false;
        }

        if (this.contextOverlaps(other))
        {
            KeyBindSettings settingsOther = other.getSettings();
            boolean o1 = this.settings.isOrderSensitive();
            boolean o2 = settingsOther.isOrderSensitive();
            List<Integer> keys1 = this.getKeys();
            List<Integer> keys2 = other.getKeys();
            int l1 = keys1.size();
            int l2 = keys2.size();

            if (l1 == 0 || l2 == 0)
            {
                return false;
            }

            if ((this.settings.getAllowExtraKeys() == false && l1 < l2 && keys1.get(0) != keys2.get(0)) ||
                (settingsOther.getAllowExtraKeys() == false && l2 < l1 && keys1.get(0) != keys2.get(0)))
            {
                return false;
            }

            // Both are order sensitive, try to "slide the shorter sequence over the longer sequence" to find a match
            if (o1 && o2)
            {
                return l1 < l2 ? Collections.indexOfSubList(keys2, keys1) != -1 : Collections.indexOfSubList(keys1, keys2) != -1;
            }
            // At least one of the keybinds is not order sensitive
            else
            {
                return l1 <= l2 ? keys2.containsAll(keys1) : keys1.containsAll(keys2);
            }
        }

        return false;
    }

    public boolean contextOverlaps(KeyBind other)
    {
        KeyBindSettings settingsOther = other.getSettings();
        Context c1 = this.settings.getContext();
        Context c2 = settingsOther.getContext();

        if (c1 == Context.ANY || c2 == Context.ANY || c1 == c2)
        {
            KeyAction a1 = this.settings.getActivateOn();
            KeyAction a2 = settingsOther.getActivateOn();

            if (a1 == KeyAction.BOTH || a2 == KeyAction.BOTH || a1 == a2)
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setValueFromJsonElement(JsonElement element, String hotkeyName)
    {
        try
        {
            if (element.isJsonObject())
            {
                JsonObject obj = element.getAsJsonObject();

                if (JsonUtils.hasString(obj, "keys"))
                {
                    this.setValueFromString(obj.get("keys").getAsString());
                }

                if (JsonUtils.hasObject(obj, "settings"))
                {
                    this.setSettings(KeyBindSettings.fromJson(obj.getAsJsonObject("settings")));
                }
            }
            // Backwards compatibility with some old hotkeys
            else if (element.isJsonPrimitive())
            {
                this.setValueFromString(element.getAsString());
            }
            else
            {
                MaLiLib.LOGGER.warn("Failed to set the hotkey '{}' from the JSON element '{}'", hotkeyName, element);
            }
        }
        catch (Exception e)
        {
            MaLiLib.LOGGER.warn("Failed to set the hotkey '{}' from the JSON element '{}'", hotkeyName, element, e);
        }

        this.cacheSavedValue();
    }

    @Override
    public JsonElement getAsJsonElement()
    {
        JsonObject obj = new JsonObject();

        String str = writeKeysToString(this.keyCodes, ",");
        obj.add("keys", new JsonPrimitive(str));

        if (this.areSettingsModified())
        {
            obj.add("settings", this.getSettings().toJson());
        }

        return obj;
    }

    public static KeyBindImpl fromStorageString(String storageString, KeyBindSettings settings)
    {
        KeyBindImpl keyBind = new KeyBindImpl(storageString, settings);
        keyBind.setValueFromString(storageString);
        return keyBind;
    }

    public static boolean isKeyDown(int keyCode)
    {
        if (keyCode > 0)
        {
            return keyCode < Keyboard.getKeyCount() && Keyboard.isKeyDown(keyCode);
        }

        keyCode += 100;

        return keyCode >= 0 && keyCode < Mouse.getButtonCount() && Mouse.isButtonDown(keyCode);
    }

    /**
     * NOT PUBLIC API - DO NOT CALL FROM MOD CODE!!!
     */
    public static void onKeyInputPre(int keyCode, boolean state)
    {
        Integer valObj = Integer.valueOf(keyCode);

        if (state)
        {
            if (PRESSED_KEYS.contains(valObj) == false)
            {
                Collection<Integer> ignored = MaLiLibConfigs.Generic.IGNORED_KEYS.getKeyBind().getKeys();

                if (ignored.size() == 0 || ignored.contains(valObj) == false)
                {
                    PRESSED_KEYS.add(valObj);
                }
            }
        }
        else
        {
            PRESSED_KEYS.remove(valObj);
        }

        if (MaLiLibConfigs.Debug.KEYBIND_DEBUG.getBooleanValue())
        {
            printKeyBindDebugMessage(keyCode, state);
        }
    }

    /**
     * NOT PUBLIC API - DO NOT CALL FROM MOD CODE!!!
     */
    public static void reCheckPressedKeys()
    {
        Iterator<Integer> iter = PRESSED_KEYS.iterator();

        while (iter.hasNext())
        {
            int keyCode = iter.next().intValue();

            if (isKeyDown(keyCode) == false)
            {
                iter.remove();
            }
        }

        // Clear the triggered count after all keys have been released
        if (PRESSED_KEYS.size() == 0)
        {
            triggeredCount = 0;
        }
    }

    private static void printKeyBindDebugMessage(int eventKey, boolean eventKeyState)
    {
        String keyName = eventKey > 0 ? Keyboard.getKeyName(eventKey) : Mouse.getButtonName(eventKey + 100);
        String type = eventKeyState ? "PRESS" : "RELEASE";
        String held = getActiveKeysString();
        String msg = String.format("%s %s (%d), held keys: %s", type, keyName, eventKey, held);

        MaLiLib.LOGGER.info(msg);

        if (MaLiLibConfigs.Debug.KEYBIND_DEBUG_ACTIONBAR.getBooleanValue())
        {
            MessageUtils.printActionbarMessage(msg);
        }
    }

    public static String getActiveKeysString()
    {
        if (PRESSED_KEYS.isEmpty() == false)
        {
            StringBuilder sb = new StringBuilder(128);
            int i = 0;

            for (int key : PRESSED_KEYS)
            {
                if (i > 0)
                {
                    sb.append(" + ");
                }

                String name = getStorageStringForKeyCode(key);

                if (name != null)
                {
                    sb.append(String.format("%s (%d)", name, key));
                }

                i++;
            }

            return sb.toString();
        }

        return "<none>";
    }

    @Nullable
    public static String getStorageStringForKeyCode(int keyCode)
    {
        if (keyCode > 0)
        {
            return Keyboard.getKeyName(keyCode);
        }
        else if (keyCode < 0)
        {
            keyCode += 100;

            if (keyCode >= 0 && keyCode < Mouse.getButtonCount())
            {
                return Mouse.getButtonName(keyCode);
            }
        }

        return null;
    }

    public static ImmutableList<Integer> readKeysFromStorageString(String str)
    {
        ArrayList<Integer> keyCodes = new ArrayList<>();
        String[] keys = str.split(",");

        for (String key : keys)
        {
            key = key.trim();

            if (key.isEmpty() == false)
            {
                int keyCode = Keyboard.getKeyIndex(key);

                if (keyCode == Keyboard.KEY_NONE)
                {
                    keyCode = Mouse.getButtonIndex(key);

                    if (keyCode >= 0 && keyCode < Mouse.getButtonCount())
                    {
                        keyCode -= 100;
                    }
                    else
                    {
                        continue;
                    }
                }

                if (keyCode != Keyboard.KEY_NONE && keyCodes.contains(keyCode) == false)
                {
                    keyCodes.add(keyCode);
                }
            }
        }

        return ImmutableList.copyOf(keyCodes);
    }

    public static String writeKeysToString(List<Integer> keyCodes, String separator)
    {
        StringBuilder sb = new StringBuilder(32);

        for (int i = 0; i < keyCodes.size(); ++i)
        {
            if (i > 0)
            {
                sb.append(separator);
            }

            int keyCode = keyCodes.get(i).intValue();
            String name = getStorageStringForKeyCode(keyCode);

            if (name != null)
            {
                sb.append(name);
            }
        }

        return sb.toString();
    }

    public static int getTriggeredCount()
    {
        return triggeredCount;
    }
}
