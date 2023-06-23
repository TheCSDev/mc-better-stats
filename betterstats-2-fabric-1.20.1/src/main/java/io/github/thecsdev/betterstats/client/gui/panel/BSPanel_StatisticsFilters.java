package io.github.thecsdev.betterstats.client.gui.panel;

import static io.github.thecsdev.betterstats.client.gui.screen.BetterStatsScreen.BSS_WIDGETS_TEXTURE;
import static io.github.thecsdev.betterstats.client.gui.screen.BetterStatsScreen.filter_showEmpty;
import static io.github.thecsdev.tcdcommons.api.util.TextUtils.literal;
import static io.github.thecsdev.tcdcommons.api.util.TextUtils.translatable;

import org.jetbrains.annotations.Nullable;

import io.github.thecsdev.betterstats.client.gui.screen.BetterStatsScreen;
import io.github.thecsdev.betterstats.client.gui.screen.BetterStatsScreen.CurrentTab;
import io.github.thecsdev.betterstats.client.gui.screen.BetterStatsScreen.GroupStatsBy;
import io.github.thecsdev.tcdcommons.api.client.gui.other.TBlankElement;
import io.github.thecsdev.tcdcommons.api.client.gui.other.TLabelElement;
import io.github.thecsdev.tcdcommons.api.client.gui.other.TTextureElement;
import io.github.thecsdev.tcdcommons.api.client.gui.util.HorizontalAlignment;
import io.github.thecsdev.tcdcommons.api.client.gui.widget.TCheckboxWidget;
import io.github.thecsdev.tcdcommons.api.client.gui.widget.TSelectEnumWidget;
import io.github.thecsdev.tcdcommons.api.client.gui.widget.TSelectWidget;
import io.github.thecsdev.tcdcommons.api.client.gui.widget.TTextFieldWidget;

public class BSPanel_StatisticsFilters extends BSPanel
{
	// ==================================================
	public @Nullable TSelectEnumWidget<CurrentTab> btn_tab;
	public @Nullable TSelectEnumWidget<GroupStatsBy> btn_groupBy;
	public @Nullable TSelectWidget btn_sortBy;
	// ==================================================
	public BSPanel_StatisticsFilters(int x, int y, int width, int height)
	{
		super(x, y, width, height);
		setSmoothScroll(true);
	}
	// ==================================================
	public void init(final BetterStatsScreen bss)
	{
		//blank placeholder element (used for getLastTChild())
		var blank = new TBlankElement(getTpeX(), getTpeY() + 5, 0, 0);
		addTChild(blank, false);
		
		//filters label
		var lbl_filters = new TLabelElement(nextX(), nextY(), nextW(), 20);
		lbl_filters.setText(translatable("betterstats.gui.filters"));
		lbl_filters.setHorizontalAlignment(HorizontalAlignment.CENTER);
		addTChild(lbl_filters, false);
		
		//tab selector
		btn_tab = new TSelectEnumWidget<>(nextX(), nextY(), nextW(), 20, CurrentTab.class);
		btn_tab.setDrawsVanillaButton(true);
		btn_tab.setSelected(bss.filter_currentTab, false);
		btn_tab.setOnSelectionChange(tab ->
		{
			bss.filter_currentTab = (CurrentTab) tab;
			bss.filter_statsScroll = 0;
			//first finish initializing the stats
			bss.getStatPanel().init_stats();
			//then re-initialize this menu
			bss.getStatPanel().init_leftMenu();
		});
		btn_tab.setEnumValueToLabel(val -> ((CurrentTab)val).asText());
		addTChild(btn_tab, false);
		
		//search bar
		var txt_search = new TTextFieldWidget(nextX(), nextY(), nextW(), 20);
		txt_search.setText(bss.filter_searchTerm, false);
		txt_search.eTextChanged.register((element, txt) ->
		{
			bss.filter_searchTerm = txt;
			bss.getStatPanel().init_stats();
		});
		addTChild(txt_search, false);
		
		//show empty stats
		var check_emptyStats = new TCheckboxWidget(nextX(), nextY(), nextW(), 20,
				translatable("betterstats.gui.show_empty_stats"), filter_showEmpty);
		check_emptyStats.eClicked.register(element ->
		{
			filter_showEmpty = check_emptyStats.getChecked();
			bss.getStatPanel().init_stats();
		});
		addTChild(check_emptyStats, false);
		
		//group by
		{
			var img_group = new TTextureElement(nextX(), nextY(), 20, 20);
			img_group.setTexture(BSS_WIDGETS_TEXTURE, 256, 256);
			img_group.setTextureUVs(0, 0, 20, 20);
			
			btn_groupBy = new TSelectEnumWidget<>(
					nextX() + 25, nextY(), nextW() - 25, 20,
					GroupStatsBy.class);
			btn_groupBy.setDrawsVanillaButton(true);
			btn_groupBy.setEnabled(bss.filter_currentTab != CurrentTab.General);
			btn_groupBy.setEnumValueToLabel(val -> ((GroupStatsBy)val).asText());
			btn_groupBy.setSelected(bss.filter_groupBy, false);
			btn_groupBy.setOnSelectionChange(newGroup ->
			{
				bss.filter_groupBy = (GroupStatsBy) newGroup;
				bss.filter_statsScroll = 0;
				bss.getStatPanel().init_stats();
			});
			
			addTChild(img_group, false);
			addTChild(btn_groupBy, false);
		}
		
		//sort by
		{
			var img_sort = new TTextureElement(nextX(), nextY(), 20, 20);
			img_sort.setTexture(BSS_WIDGETS_TEXTURE, 256, 256);
			img_sort.setTextureUVs(0, 20, 20, 20);
			
			btn_sortBy = (bss.getStatPanel().getCurrentStatPanel() != null) ?
					bss.getStatPanel().getCurrentStatPanel()
						.createFilterSortByWidget(bss, nextX() + 25, nextY(), nextW() - 25, 20) :
					null;
			if(btn_sortBy == null)
			{
				btn_sortBy = new TSelectWidget(nextX() + 25, nextY(), nextW() - 25, 20);
				btn_sortBy.setEnabled(false);
				btn_sortBy.addDropdownOption(literal("-"), null);
				btn_sortBy.setMessage(literal("-"));
			}
			btn_sortBy.setDrawsVanillaButton(true);
			
			addTChild(img_sort, false);
			addTChild(btn_sortBy, false);
		}
		
		//remove the blank element as it is no longer needed
		removeTChild(blank);
	}
	// ==================================================
	public int nextX() { return getTpeX() + getScrollPadding(); }
	public int nextY() { return getLastTChild(false).getTpeEndY() + 5; }
	public int nextW() { return getTpeWidth() - (getScrollPadding() * 2); }
	// ==================================================
}