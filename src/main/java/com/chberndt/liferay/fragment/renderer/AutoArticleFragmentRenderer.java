package com.chberndt.liferay.fragment.renderer;

import com.chberndt.liferay.fragment.renderer.util.FragmentRendererUtil;

import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.fragment.renderer.FragmentRendererContext;
import com.liferay.fragment.util.configuration.FragmentEntryConfigurationParser;
import com.liferay.info.item.ClassPKInfoItemIdentifier;
import com.liferay.info.item.InfoItemServiceTracker;
import com.liferay.info.item.provider.InfoItemObjectProvider;
import com.liferay.info.item.renderer.InfoItemRenderer;
import com.liferay.info.item.renderer.InfoItemRendererTracker;
import com.liferay.info.item.renderer.InfoItemTemplatedRenderer;
import com.liferay.journal.constants.JournalArticleConstants;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.Tuple;

import java.io.File;
import java.io.IOException;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Christian Berndt
 *
 * After the model of com.liferay.fragment.internal.renderer.ContentObjectFragmentRenderer
 */
@Component(service = FragmentRenderer.class)
public class AutoArticleFragmentRenderer implements FragmentRenderer {

	@Override
	public String getCollectionKey() {
		return "content-display";
	}

	@Override
	public String getConfiguration(
		FragmentRendererContext fragmentRendererContext) {

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		JSONObject configurationObject = JSONFactoryUtil.createJSONObject();

		configurationObject.put(
			"fieldSets",
			JSONUtil.putAll(
				JSONUtil.put(
					"fields",
					JSONUtil.putAll(
						JSONUtil.put(
							"label", "content-display"
						).put(
							"name", "itemSelector"
						).put(
							"type", "itemSelector"
						).put(
							"typeOptions",
							JSONUtil.put(
								"enableSelectTemplate", true
							).put(
								"itemType", JournalArticle.class.getName()
							)
						)))));

		JSONObject jsonObject =
			(JSONObject)_fragmentEntryConfigurationParser.getFieldValue(
				configurationObject.toString(),
				fragmentEntryLink.getEditableValues(), "itemSelector");

		if (jsonObject == null) {

			// If no article has been configured yet, create an new Intro / Teaser Article
			// and pass it's configuration parameters ("classPK") to the configuration object

			System.out.println(
				"fragmentEntryLink.getPlid() = " + fragmentEntryLink.getPlid());

			try {

				// Obtain the current Layout's uuid

				Layout layout = _layoutLocalService.getLayout(
					fragmentEntryLink.getPlid());

				String articleId = layout.getUuid();

				long groupId = fragmentEntryLink.getGroupId();

				JournalArticle article =
					_journalArticleLocalService.fetchArticle(
						groupId, articleId);

				if (article == null) {
					String layoutName = layout.getName(
						LocaleUtil.getSiteDefault());

					// Create a new JournalArticle with the current Layout's uuid

					Company company = _companyLocalService.getCompany(
						fragmentEntryLink.getCompanyId());

					User user = company.getDefaultUser();

					long userId = user.getUserId();

					long folderId = 0; // TODO: Obtain folderId of teaser / intro articles
					long classNameId =
						JournalArticleConstants.CLASS_NAME_ID_DEFAULT;
					long classPK = 0;
					boolean autoArticleId = false;
					double version = 0;
					Map<Locale, String> titleMap = HashMapBuilder.put(
						LocaleUtil.getSiteDefault(), "Teaser " + layoutName
					).build();
					Map<Locale, String> descriptionMap = null;
					String content = _getContent();
					String ddmStructureKey = "38757"; // TODO: Obtain key of intro / teaser structure
					String ddmTemplateKey = StringPool.BLANK; // TODO: Obtain key of default intro / teaser template
					String layoutUuid = null;

					Calendar now = Calendar.getInstance();

					int displayDateDay = now.get(Calendar.DAY_OF_WEEK);
					int displayDateHour = now.get(Calendar.HOUR_OF_DAY);
					int displayDateMinute = now.get(Calendar.MINUTE);
					int displayDateMonth = now.get(Calendar.MONTH);
					int displayDateYear = now.get(Calendar.YEAR);

					int expirationDateDay = 0;
					int expirationDateHour = 0;
					int expirationDateMinute = 0;
					int expirationDateMonth = 0;
					int expirationDateYear = 0;

					boolean neverExpire = true;

					int reviewDateDay = 0;
					int reviewDateHour = 0;
					int reviewDateMinute = 0;
					int reviewDateMonth = 0;
					int reviewDateYear = 0;

					boolean neverReview = true;

					boolean indexable = true;

					boolean smallImage = false;
					File smallImageFile = null;
					String smallImageURL = StringPool.BLANK;
					Map<String, byte[]> images = null;
					String articleURL = StringPool.BLANK;

					ServiceContext serviceContext = new ServiceContext();

					serviceContext.setScopeGroupId(groupId);

					article = _journalArticleLocalService.addArticle(
						userId, groupId, folderId, classNameId, classPK,
						articleId, autoArticleId, version, titleMap,
						descriptionMap, content, ddmStructureKey,
						ddmTemplateKey, layoutUuid, displayDateMonth,
						displayDateDay, displayDateYear, displayDateHour,
						displayDateMinute, expirationDateMonth,
						expirationDateDay, expirationDateYear,
						expirationDateHour, expirationDateMinute, neverExpire,
						reviewDateMonth, reviewDateDay, reviewDateYear,
						reviewDateHour, reviewDateMinute, neverReview,
						indexable, smallImage, smallImageURL, smallImageFile,
						images, articleURL, serviceContext);

					// TODO: Store the new article's parameters in the configuration object

				}
			}
			catch (PortalException e) {
				_log.error(e.getMessage());
			}
		}

		return configurationObject.toString();
	}

	@Override
	public String getIcon() {
		return "web-content";
	}

	@Override
	public String getLabel(Locale locale) {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", getClass());

		return LanguageUtil.get(resourceBundle, "auto-article-display");
	}

	@Override
	public void render(
			FragmentRendererContext fragmentRendererContext,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException {

		JSONObject jsonObject = _getFieldValueJSONObject(
			fragmentRendererContext);

		Optional<Object> displayObjectOptional =
			fragmentRendererContext.getDisplayObjectOptional();

		if (!displayObjectOptional.isPresent() &&
			((jsonObject == null) || (jsonObject.length() == 0))) {

			if (FragmentRendererUtil.isEditMode(httpServletRequest)) {
				FragmentRendererUtil.printPortletMessageInfo(
					httpServletRequest, httpServletResponse,
					"the-selected-content-will-be-shown-here");
			}

			return;
		}

		Object displayObject = null;

		if (jsonObject != null) {
			displayObject = _getDisplayObject(
				jsonObject.getString("className"),
				jsonObject.getLong("classPK"), displayObjectOptional);
		}
		else {
			displayObject = displayObjectOptional.orElse(null);
		}

		if (displayObject == null) {
			if (FragmentRendererUtil.isEditMode(httpServletRequest)) {
				FragmentRendererUtil.printPortletMessageInfo(
					httpServletRequest, httpServletResponse,
					"the-selected-content-is-no-longer-available.-please-" +
						"select-another");
			}

			return;
		}

		Tuple tuple = _getTuple(
			displayObject.getClass(), fragmentRendererContext);

		InfoItemRenderer<Object> infoItemRenderer =
			(InfoItemRenderer<Object>)tuple.getObject(0);

		if (infoItemRenderer == null) {
			if (FragmentRendererUtil.isEditMode(httpServletRequest)) {
				FragmentRendererUtil.printPortletMessageInfo(
					httpServletRequest, httpServletResponse,
					"there-are-no-available-renderers-for-the-selected-" +
						"content");
			}

			return;
		}

		if (infoItemRenderer instanceof InfoItemTemplatedRenderer) {
			InfoItemTemplatedRenderer<Object> infoItemTemplatedRenderer =
				(InfoItemTemplatedRenderer<Object>)infoItemRenderer;

			if (tuple.getSize() > 1) {
				infoItemTemplatedRenderer.render(
					displayObject, (String)tuple.getObject(1),
					httpServletRequest, httpServletResponse);
			}
			else {
				infoItemTemplatedRenderer.render(
					displayObject, httpServletRequest, httpServletResponse);
			}
		}
		else {
			infoItemRenderer.render(
				displayObject, httpServletRequest, httpServletResponse);
		}
	}

	private String _getContent() {
		StringBundler sb = new StringBundler(10);

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append(
			"<root available-locales=\"en_US\" default-locale=\"en_US\">\n");
		sb.append(
			"\t<dynamic-element name=\"Teaser\" type=\"text\" index-type=\"keyword\" instance-id=\"udlhvppi\">\n");
		//		sb.append("<dynamic-element name=\"Headline\" instance-id=\"rsrxtqtg\" type=\"text\" index-type=\"keyword\">");
		//		sb.append("<dynamic-content language-id=\"en_US\"><![CDATA[Intro About Us]]></dynamic-content>");
		//		sb.append("</dynamic-element>");
		//		sb.append("<dynamic-element name=\"Description\" instance-id=\"vcstwmcj\" type=\"text_box\" index-type=\"text\">");
		//		sb.append("<dynamic-content language-id=\"en_US\"><![CDATA[This is the intro / default teaser for the About Us page from the Site Template. This article is stored in Global.]]></dynamic-content>");
		//		sb.append("</dynamic-element>");
		sb.append(
			"\t\t<dynamic-content language-id=\"en_US\"><![CDATA[Intro]]></dynamic-content>\n");
		sb.append("\t</dynamic-element>\n");
		sb.append("</root>");

		return sb.toString();
	}

	private Object _getDisplayObject(
		String className, long classPK,
		Optional<Object> displayObjectOptional) {

		InfoItemObjectProvider<?> infoItemObjectProvider =
			_infoItemServiceTracker.getFirstInfoItemService(
				InfoItemObjectProvider.class, className);

		if (infoItemObjectProvider == null) {
			return displayObjectOptional.orElse(null);
		}

		try {
			Object infoItem = infoItemObjectProvider.getInfoItem(
				new ClassPKInfoItemIdentifier(classPK));

			if (infoItem == null) {
				return displayObjectOptional.orElse(null);
			}

			return infoItem;
		}
		catch (Exception exception) {
		}

		return displayObjectOptional.orElse(null);
	}

	private JSONObject _getFieldValueJSONObject(
		FragmentRendererContext fragmentRendererContext) {

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		return (JSONObject)_fragmentEntryConfigurationParser.getFieldValue(
			getConfiguration(fragmentRendererContext),
			fragmentEntryLink.getEditableValues(), "itemSelector");
	}

	private Tuple _getTuple(
		Class<?> displayObjectClass,
		FragmentRendererContext fragmentRendererContext) {

		List<InfoItemRenderer<?>> infoItemRenderers =
			FragmentRendererUtil.getInfoItemRenderers(
				displayObjectClass, _infoItemRendererTracker);

		if (infoItemRenderers == null) {
			return null;
		}

		InfoItemRenderer<Object> defaultInfoItemRenderer =
			(InfoItemRenderer<Object>)infoItemRenderers.get(0);

		JSONObject jsonObject = _getFieldValueJSONObject(
			fragmentRendererContext);

		if (jsonObject == null) {
			return new Tuple(defaultInfoItemRenderer);
		}

		JSONObject templateJSONObject = jsonObject.getJSONObject("template");

		if (templateJSONObject == null) {
			return new Tuple(defaultInfoItemRenderer);
		}

		String infoItemRendererKey = templateJSONObject.getString(
			"infoItemRendererKey");

		InfoItemRenderer<Object> infoItemRenderer =
			(InfoItemRenderer<Object>)
				_infoItemRendererTracker.getInfoItemRenderer(
					infoItemRendererKey);

		if (infoItemRenderer != null) {
			return new Tuple(
				infoItemRenderer, templateJSONObject.getString("templateKey"));
		}

		return new Tuple(defaultInfoItemRenderer);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AutoArticleFragmentRenderer.class.getName());

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private InfoItemRendererTracker _infoItemRendererTracker;

	@Reference
	private InfoItemServiceTracker _infoItemServiceTracker;

	@Reference
	private JournalArticleLocalService _journalArticleLocalService;

	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private Portal _portal;

}