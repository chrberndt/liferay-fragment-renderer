<#assign
	parameterNames = request.getParameterNames()
	attributeNames = request.getAttributeNames()
	instanceId = ""

	groupId = getterUtil.getLong(themeDisplay.getScopeGroupId())
	articleId = getterUtil.getString(.vars['reserved-article-id'].data)
/>

<#if request.getAttribute("instanceId")??>
	<#assign instanceId = request.getAttribute("instanceId") />
</#if>

<#if journalArticleUtil??>
	<#if journalArticleUtil.getArticle(groupId, articleId)??>
		<#assign article = journalArticleUtil.getArticle(groupId, articleId) />
	</#if>
</#if>

<#assign
	headline = "Headline not available"
	description = "Description not available"
/>

<#if article??>

	<#-- TODO: retrieve related layout based on the articleId -->

	<#assign content = saxReaderUtil.read(article.getContent()) />
	<#if instanceId?? && instanceId?has_content>
		<#if journalArticleUtil.getArticleNode(groupId, articleId, instanceId)??>
			<#assign teaser = journalArticleUtil.getArticleNode(groupId, articleId, instanceId) />
			<#if teaser??>
				<#assign
					headline = teaser.selectSingleNode("./dynamic-element[@name='Headline']").getStringValue()
					description = teaser.selectSingleNode("./dynamic-element[@name='Description']").getStringValue()
				/>
			</#if>
		</#if>
	</#if>
</#if>

<#if article??>
	<div class="card card-type-asset image-card">
		<div class="card-item-first">
			<svg class="bd-placeholder-img card-img-top" width="100%" height="180" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Placeholder: Image cap" preserveAspectRatio="xMidYMid slice" focusable="false"><title>Placeholder</title><rect width="100%" height="100%" fill="#6c757d"></rect><text x="50%" y="50%" fill="#dee2e6" dy=".3em">Image cap</text></svg>
		</div>

		<div class="card-body">
			<h1>${headline}</h1>

			<p class="card-text lead">${description}</p>

			<a href="#" class="btn btn-primary">Learn More</a>
		</div>
	</div>
<#else>
	<div class="alert alert-warning" role="alert">
		<span class="alert-indicator"></span>

		<strong class="lead">Warning: </strong>The requested article could not be loaded. Please make sure the journalArticleUtil has been deployed with the respective ThemeContextContributor.
	</div>
</#if>