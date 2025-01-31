package malilib.gui.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiScreen;

import malilib.MaLiLibConfigs;
import malilib.config.option.ConfigInfo;
import malilib.config.util.ConfigUtils;
import malilib.gui.BaseScreen;
import malilib.gui.tab.BaseScreenTab;
import malilib.util.data.ModInfo;

public class BaseConfigTab extends BaseScreenTab implements ConfigTab
{
    protected final List<? extends ConfigInfo> configs;
    protected final ModInfo modInfo;
    protected final int configWidth;

    public BaseConfigTab(ModInfo modInfo, String name, int configWidth,
                         List<? extends ConfigInfo> configs, Supplier<BaseScreen> screenFactory)
    {
        this(modInfo, name, modInfo.getModId() + ".config.tab." + name, configWidth, configs, screenFactory);
    }

    public BaseConfigTab(ModInfo modInfo, String name, String translationKey, int configWidth,
                         List<? extends ConfigInfo> configs, Supplier<BaseScreen> screenFactory)
    {
        // The current screen is also a config screen, so a simple tab switch is enough
        this(modInfo, name, translationKey, configWidth, configs,
             (scr) -> scr instanceof BaseConfigScreen, screenFactory);
    }

    public BaseConfigTab(ModInfo modInfo, String name, String translationKey, int configWidth,
                         List<? extends ConfigInfo> configs,
                         Predicate<GuiScreen> screenChecker,
                         Supplier<BaseScreen> screenFactory)
    {
        super(name, translationKey, screenChecker, screenFactory);

        this.modInfo = modInfo;
        this.configWidth = configWidth;
        this.configs = configs;

        this.setModInfoToConfigsIfNotSet();
    }

    @Override
    public ModInfo getModInfo()
    {
        return this.modInfo;
    }

    @Override
    public int getConfigWidgetsWidth()
    {
        return this.configWidth;
    }

    @Override
    public List<? extends ConfigInfo> getConfigs()
    {
        if (MaLiLibConfigs.Generic.SORT_CONFIGS_BY_NAME.getBooleanValue())
        {
            ArrayList<ConfigInfo> list = new ArrayList<>(this.configs);
            ConfigUtils.sortConfigsByDisplayName(list);
            return list;
        }

        return ConfigUtils.getExtendedList(this.configs);
    }

    protected void setModInfoToConfigsIfNotSet()
    {
        for (ConfigInfo cfg : this.configs)
        {
            if (cfg.getModInfo() == null)
            {
                cfg.setModInfo(this.modInfo);
            }
        }
    }
}
