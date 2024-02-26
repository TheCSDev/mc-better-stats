package io.github.thecsdev.betterstats.client.gui.stats.tabs;

import static io.github.thecsdev.betterstats.client.BetterStatsClient.MC_CLIENT;
import static io.github.thecsdev.tcdcommons.api.util.TextUtils.literal;
import static io.github.thecsdev.tcdcommons.api.util.TextUtils.translatable;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.thecsdev.betterstats.BetterStats;
import io.github.thecsdev.betterstats.api.client.gui.util.StatsTabUtils;
import io.github.thecsdev.betterstats.api.client.registry.StatsTab;
import io.github.thecsdev.betterstats.client.gui.widget.CreditsTabPersonWidget;
import io.github.thecsdev.betterstats.util.BST;
import io.github.thecsdev.tcdcommons.api.client.gui.config.TConfigPanelBuilder;
import io.github.thecsdev.tcdcommons.api.client.gui.other.TLabelElement;
import io.github.thecsdev.tcdcommons.api.client.gui.panel.TPanelElement;
import io.github.thecsdev.tcdcommons.api.client.gui.util.TDrawContext;
import io.github.thecsdev.tcdcommons.api.util.enumerations.HorizontalAlignment;
import io.github.thecsdev.tcdcommons.util.io.http.TcdCommonsWebApi;
import io.github.thecsdev.tcdcommons.util.io.http.TcdWebApiPerson;
import net.minecraft.text.Text;

public final class BSCreditsTab extends StatsTab
{
	// ==================================================
	private static final Text TEXT_TITLE          = translatable("credits_and_attribution.button.credits");
	private static final Text TEXT_CONTRIBUTORS   = BST.bss_contributors_title();
	private static final Text TEXT_SPONSORS       = translatable("tcdcommons.github.sponsors");
	private static final Text TEXT_SPECIAL_THANKS = translatable("tcdcommons.special_thanks");
	private static final Text TEXT_FEATURED       = translatable("tcdcommons.featured");
	private static final Text TEXT_FEATURED_NOONE = translatable("tcdcommons.featured.no_one");
	private static final Text TEXT_FETCH_FAIL     = translatable("tcdcommons.fetch_fail");
	// ==================================================
	public final @Override Text getName() { return TEXT_TITLE; }
	public final @Override boolean isAvailable() { return false; }
	// --------------------------------------------------
	public final @Override void initFilters(FiltersInitContext initContext) { StatsTabUtils.initDefaultFilters(initContext); }
	// ==================================================
	public final @Override void initStats(final StatsInitContext initContext)
	{
		final var panel = initContext.getStatsPanel();
		
		final var lbl_title = StatsTabUtils.initGroupLabel(panel, TEXT_TITLE);
		lbl_title.setSize(lbl_title.getWidth(), lbl_title.getHeight() * 2);
		lbl_title.setTextColor(TDrawContext.DEFAULT_TEXT_COLOR);
		lbl_title.setTextScale(1.8f);
		lbl_title.setTextHorizontalAlignment(HorizontalAlignment.CENTER);
		
		//group label for translators
		initGroupLabel(panel, TEXT_CONTRIBUTORS);
		{
			final var contributors = getContributors();
			boolean highlight = true;
			int iteration = 1;
			
			for(final var contributor : contributors)
			{
				//keep track of the loop iterations
				highlight = !highlight;
				iteration++;
				
				//create gui elements
				final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
				final var ctpw = new CreditsTabPersonWidget(n1.x, n1.y, n1.width, contributor, iteration <= 10);
				ctpw.setBackgroundColor(highlight ? 0x44000000 : 0x22000000);
				panel.addChild(ctpw, false);
			}
		}
		
		//trigger sponsor segment
		initSponsorSegment(initContext);
	}
	// --------------------------------------------------
	private final void initSponsorSegment(final StatsInitContext initContext)
	{
		final var panel = initContext.getStatsPanel();
		initGroupLabel(panel, literal("").append(TEXT_SPONSORS).append(" (").append(TEXT_FEATURED).append(")"));
		TcdCommonsWebApi.getFeaturedSponsorsAsync(
			MC_CLIENT,
			jsonArr ->
			{
				boolean highlight = true;
				int iteration = 1;
				if(jsonArr.isEmpty())
				{
					final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
					final var lbl = new TLabelElement(n1.x, n1.y, n1.width, 20, TEXT_FEATURED_NOONE);
					lbl.setTextHorizontalAlignment(HorizontalAlignment.CENTER);
					panel.addChild(lbl, false);
				}
				else for(final var personJson : jsonArr)
				{
					if(!personJson.isJsonObject()) continue;
					final @Nullable var person = tryParsePerson(personJson.getAsJsonObject());
					if(person == null) continue;
					
					iteration++;
					highlight = !highlight;
					final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
					final var ctpw = new CreditsTabPersonWidget(n1.x, n1.y, n1.width, person, iteration <= 10);
					ctpw.setBackgroundColor(highlight ? 0x44000000 : 0x22000000);
					panel.addChild(ctpw, false);
				};
				
				initSpecialThanksSegment(initContext);
			},
			err ->
			{
				final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
				final var lbl = new TLabelElement(n1.x, n1.y, n1.width, 20, TEXT_FETCH_FAIL);
				lbl.setTextHorizontalAlignment(HorizontalAlignment.CENTER);
				panel.addChild(lbl, false);
				
				initSpecialThanksSegment(initContext);
			});
	}
	// --------------------------------------------------
	private final void initSpecialThanksSegment(final StatsInitContext initContext)
	{
		final var panel = initContext.getStatsPanel();
		initGroupLabel(panel, TEXT_SPECIAL_THANKS);
		TcdCommonsWebApi.getSpecialThanksAsync(
			MC_CLIENT,
			jsonArr ->
			{
				boolean highlight = true;
				int iteration = 1;
				if(jsonArr.isEmpty())
				{
					final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
					final var lbl = new TLabelElement(n1.x, n1.y, n1.width, 20, TEXT_FEATURED_NOONE);
					lbl.setTextHorizontalAlignment(HorizontalAlignment.CENTER);
					panel.addChild(lbl, false);
				}
				else for(final var personJson : jsonArr)
				{
					if(!personJson.isJsonObject()) continue;
					final @Nullable var person = tryParsePerson(personJson.getAsJsonObject());
					if(person == null) continue;
					
					iteration++;
					highlight = !highlight;
					final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
					final var ctpw = new CreditsTabPersonWidget(n1.x, n1.y, n1.width, person, iteration <= 10);
					ctpw.setBackgroundColor(highlight ? 0x44000000 : 0x22000000);
					panel.addChild(ctpw, false);
				};
			},
			err ->
			{
				final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
				final var lbl = new TLabelElement(n1.x, n1.y, n1.width, 20, TEXT_FETCH_FAIL);
				lbl.setTextHorizontalAlignment(HorizontalAlignment.CENTER);
				panel.addChild(lbl, false);
			});
	}
	// ==================================================
	private static final void initGroupLabel(TPanelElement panel, Text text)
	{
		final var n1 = TConfigPanelBuilder.nextPanelVerticalRect(panel);
		final var panel_tGroup = new TPanelElement(n1.x, n1.y, n1.width, 20);
		panel_tGroup.setBackgroundColor(0xff353535);
		panel_tGroup.setOutlineColor(0xff000000);
		panel.addChild(panel_tGroup, false);
		//panel_tGroup.addChild(new TFillColorElement(2, 2, 16, 16, panel_tGroup.getOutlineColor()));
		final var lbl = new TLabelElement(
				panel_tGroup.getHeight() + 5, 0,
				panel_tGroup.getWidth(), panel_tGroup.getHeight(),
				text);
		lbl.setTextHorizontalAlignment(HorizontalAlignment.LEFT);
		lbl.setTextColor(0xffffff00);
		lbl.setTextScale(0.8f);
		panel_tGroup.addChild(lbl);
	}
	// ==================================================
	/**
	 * Returns an array of {@link TcdWebApiPerson}s who are credited as contributors of this mod.
	 */
	public static final TcdWebApiPerson[] getContributors()
	{
		//obtain the contributors array
		@Nullable JsonArray contributors = null;
		try { contributors = BetterStats.getModProperties().get("contributors").getAsJsonArray(); }
		catch(Exception e) { return new TcdWebApiPerson[] {}; }
		
		//return
		return contributors.asList().stream()
			.filter(p -> p.isJsonObject())
			.map(p -> p.getAsJsonObject())
			.map(p -> tryParsePerson(p))
			.filter(p -> p != null)
			.toArray(TcdWebApiPerson[]::new);
	}
	// --------------------------------------------------
	/**
	 * Parses a {@link JsonObject} to a {@link TcdWebApiPerson}.
	 */
	private static final @Nullable TcdWebApiPerson tryParsePerson(JsonObject json) throws NullPointerException
	{
		try { return new TcdWebApiPerson(json); } catch(IOException e) { return null; }
	}
	// ==================================================
}