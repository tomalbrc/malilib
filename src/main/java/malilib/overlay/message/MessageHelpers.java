package malilib.overlay.message;

import java.util.Locale;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import malilib.config.option.BooleanContainingConfig;
import malilib.util.StringUtils;

public class MessageHelpers
{
    public static String getOnOff(boolean value, boolean capitalize)
    {
        String key;

        if (capitalize)
        {
            key = value ? "malilibdev.label.misc.on.caps" : "malilibdev.label.misc.off.caps";
        }
        else
        {
            key = value ? "malilibdev.label.misc.on" : "malilibdev.label.misc.off";
        }

        return StringUtils.translate(key);
    }

    public static String getOnOffColored(boolean value, boolean capitalize)
    {
        String key;

        if (capitalize)
        {
            key = value ? "malilibdev.label.misc.on.caps_colored" : "malilibdev.label.misc.off.caps_colored";
        }
        else
        {
            key = value ? "malilibdev.label.misc.on.colored" : "malilibdev.label.misc.off.colored";
        }

        return StringUtils.translate(key);
    }

    public static String getTrueFalse(boolean value, boolean capitalize)
    {
        String key = value ? "malilibdev.label.misc.true.lower_case" : "malilibdev.label.misc.false.lower_case";
        return translateAndCapitalize(key, capitalize);
    }

    public static String getTrueFalseColored(boolean value, boolean capitalize)
    {
        String key = value ? "malilibdev.label.misc.true.lower_case.colored" : "malilibdev.label.misc.false.lower_case.colored";
        return translateAndCapitalize(key, capitalize);
    }

    public static String getYesNo(boolean value, boolean capitalize)
    {
        String key = value ? "malilibdev.label.misc.yes" : "malilibdev.label.misc.no";
        return translateAndCapitalize(key, capitalize);
    }

    public static String getYesNoColored(boolean value, boolean capitalize)
    {
        String key = value ? "malilibdev.label.misc.yes.colored" : "malilibdev.label.misc.no.colored";
        return translateAndCapitalize(key, capitalize);
    }

    public static String translateAndCapitalize(String key, boolean capitalize)
    {
        String str = StringUtils.translate(key);
        return capitalize ? str.toUpperCase(Locale.ROOT) : str;
    }

    public static String getBooleanConfigToggleMessage(BooleanContainingConfig<?> config,
                                                       @Nullable BooleanConfigMessageFactory messageFactory)
    {
        boolean newValue = config.getBooleanValue();
        String message;

        if (config.hasOverride())
        {
            String msgKey = newValue ? "malilibdev.message.info.config_overridden_on" :
                                       "malilibdev.message.info.config_overridden_off";
            message = StringUtils.translate(msgKey, config.getPrettyName());
        }
        else if (config.isLocked())
        {
            String msgKey = newValue ? "malilibdev.message.info.config_locked_on" :
                                       "malilibdev.message.info.config_locked_off";
            message = StringUtils.translate(msgKey, config.getPrettyName());
        }
        else if (messageFactory != null)
        {
            message = messageFactory.getMessage(config);
        }
        else
        {
            message = getBasicBooleanConfigToggleMessage(config);
        }

        return message;
    }

    public static String getBasicBooleanConfigToggleMessage(BooleanContainingConfig<?> config)
    {
        String msgKey = config.getBooleanValue() ? "malilibdev.message.info.toggled_config_on" :
                                                   "malilibdev.message.info.toggled_config_off";
        return StringUtils.translate(msgKey, config.getPrettyName());
    }

    public interface BooleanConfigMessageFactory
    {
        String getMessage(BooleanContainingConfig<?> config);
    }

    public static class SimpleBooleanConfigMessageFactory implements BooleanConfigMessageFactory
    {
        protected final String translationKey;
        protected final Supplier<String> valueFactory;

        public SimpleBooleanConfigMessageFactory(String translationKey, Supplier<String> valueFactory)
        {
            this.translationKey = translationKey;
            this.valueFactory = valueFactory;
        }

        @Override
        public String getMessage(BooleanContainingConfig<?> config)
        {
            if (config.getBooleanValue())
            {
                return StringUtils.translate(this.translationKey, this.valueFactory.get());
            }
            else
            {
                return MessageHelpers.getBasicBooleanConfigToggleMessage(config);
            }
        }
    }
}
