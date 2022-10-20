package malilib.gui.widget.list.entry.config.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import malilib.config.option.list.StatusEffectListConfig;
import malilib.gui.config.ConfigWidgetContext;
import malilib.gui.widget.button.BaseValueListEditButton;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.util.StringUtils;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.util.registry.Registry;

public class StatusEffectListConfigWidget extends BaseValueListConfigWidget<Potion, StatusEffectListConfig>
{
    public StatusEffectListConfigWidget(StatusEffectListConfig config,
                                        DataListEntryWidgetData constructData,
                                        ConfigWidgetContext ctx)
    {
        super(config, constructData, ctx);
    }

    @Override
    protected GenericButton createButton(int width, int height, StatusEffectListConfig config, ConfigWidgetContext ctx)
    {
        String title = StringUtils.translate("malilibdev.title.screen.status_effect_list_edit", this.config.getDisplayName());

        return new BaseValueListEditButton<>(width, height,
                                             config,
                                             this::updateWidgetState,
                                             () -> Potions.REGENERATION,
                                             StatusEffectListConfigWidget::getSortedEffectList,
                                             StatusEffectListConfig::getRegistryName,
                                             null,
                                             title);
    }

    public static List<Potion> getSortedEffectList()
    {
        List<Potion> effects = new ArrayList<>();

        for (Potion effect : Registry.POTION)
        {
            effects.add(effect);
        }

        effects.sort(Comparator.comparing(StatusEffectListConfig::getRegistryName));

        return effects;
    }
}
