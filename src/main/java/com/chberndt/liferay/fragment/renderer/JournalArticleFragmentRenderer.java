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
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.Tuple;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;

import java.util.List;
import java.util.Locale;
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
public class JournalArticleFragmentRenderer implements FragmentRenderer {

	@Override
	public String getCollectionKey() {
		return "content-display";
	}

	@Override
	public String getConfiguration(
		FragmentRendererContext fragmentRendererContext) {

		// itemSelector: default Web Content picker

		JSONObject itemSelector = JSONFactoryUtil.createJSONObject();

		itemSelector.put(
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
		);

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		// Obtain id of selected article from the configuration

		long classPK = 0;

		try {
			JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
				fragmentEntryLink.getEditableValues());

			JSONObject freemarkerEntryProcessor =
				JSONFactoryUtil.createJSONObject(
					jsonObject.getString(
						"com.liferay.fragment.entry.processor.freemarker." +
							"FreeMarkerFragmentEntryProcessor"));

			JSONObject itemSelectorConfig = JSONFactoryUtil.createJSONObject(
				freemarkerEntryProcessor.getString("itemSelector"));

			classPK = itemSelectorConfig.getLong("classPK");
		}
		catch (JSONException e) {
			_log.error(e.getMessage());
		}

		// Load configured article

		if (classPK > 0) {
			try {
				JournalArticle article =
					_journalArticleLocalService.getLatestArticle(classPK);

				Document document = SAXReaderUtil.read(article.getContent());

				Element root = document.getRootElement();

				List<Node> nodes = root.selectNodes("./dynamic-element");

				// instanceId: custom Web-Content node picker

				JSONObject instanceId = JSONFactoryUtil.createJSONObject();

				// Obtain list of teasers from article

				JSONArray validValues = _getInstanceIdConfig(nodes);

				instanceId.put(
					"defaultValue", "TODO: read from nodes[0]"
				).put(
					"label", "Teasers"
				).put(
					"name", "instanceId"
				).put(
					"type", "select"
				).put(
					"typeOptions", JSONUtil.put("validValues", validValues)
				);

				return JSONUtil.put(
					"fieldSets",
					JSONUtil.putAll(
						JSONUtil.put(
							"fields",
							JSONUtil.putAll(itemSelector, instanceId)))
				).toString();
			}
			catch (PortalException e) {
				_log.error(e.getMessage());

				return null;
			}
			catch (DocumentException e) {
				_log.error(e.getMessage());

				return null;
			}
		}

		return JSONUtil.put(
			"fieldSets",
			JSONUtil.putAll(
				JSONUtil.put("fields", JSONUtil.putAll(itemSelector)))
		).toString();
	}

	@Override
	public String getIcon() {
		return "web-content";
	}

	@Override
	public String getLabel(Locale locale) {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", getClass());

		return LanguageUtil.get(resourceBundle, "custom-display");
	}

	@Override
	public void render(
		FragmentRendererContext fragmentRendererContext,
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {

		JSONObject jsonObject = _getFieldValueJSONObject(
			fragmentRendererContext);

		try {
			FragmentEntryLink fragmentEntryLink =
				fragmentRendererContext.getFragmentEntryLink();

			JSONObject editableValues = JSONFactoryUtil.createJSONObject(
				fragmentEntryLink.getEditableValues());

			JSONObject freemarkerEntryProcessor =
				JSONFactoryUtil.createJSONObject(
					editableValues.getString(
						"com.liferay.fragment.entry.processor.freemarker." +
							"FreeMarkerFragmentEntryProcessor"));

			String instanceId = freemarkerEntryProcessor.getString(
				"instanceId");

			// Pass the inststance as a request attribute

			httpServletRequest.setAttribute("instanceId", instanceId);
		}
		catch (JSONException e) {
			_log.error(e.getMessage());
		}

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

	private JSONArray _getInstanceIdConfig(List<Node> nodes) {
		JSONArray validValues = JSONFactoryUtil.createJSONArray();

		for (Node node : nodes) {
			Node content = node.selectSingleNode("./dynamic-content");

			String instanceId = (String)node.selectObject(
				"string(./@instance-id)");

			validValues.put(
				JSONUtil.put(
					"label", content.getStringValue()
				).put(
					"value", instanceId
				));
		}

		return validValues;
	}

	private Tuple _getTuple(
		Class<?> displayObjectClass,
		FragmentRendererContext fragmentRendererContext) {

		// TODO: Skip this, since we know in advance it's a JournalArticle

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
		JournalArticleFragmentRenderer.class.getName());

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private InfoItemRendererTracker _infoItemRendererTracker;

	@Reference
	private InfoItemServiceTracker _infoItemServiceTracker;

	@Reference
	private JournalArticleLocalService _journalArticleLocalService;

}