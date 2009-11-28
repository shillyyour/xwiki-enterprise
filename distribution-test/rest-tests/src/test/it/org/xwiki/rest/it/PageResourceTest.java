/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rest.it;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.jackrabbit.uuid.UUID;
import org.xwiki.rest.Relations;
import org.xwiki.rest.it.framework.AbstractHttpTest;
import org.xwiki.rest.it.framework.TestConstants;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.model.jaxb.Spaces;
import org.xwiki.rest.model.jaxb.Syntaxes;
import org.xwiki.rest.model.jaxb.Translation;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.SyntaxesResource;
import org.xwiki.rest.resources.pages.PageChildrenResource;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.wikis.WikisResource;

public class PageResourceTest extends AbstractHttpTest
{
    private Page getFirstPage() throws Exception
    {
        GetMethod getMethod = executeGet(getFullUri(WikisResource.class));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(wikis.getWikis().size() > 0);

        Wiki wiki = wikis.getWikis().get(0);
        Link link = getFirstLinkByRelation(wiki, Relations.SPACES);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Spaces spaces = (Spaces) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertTrue(spaces.getSpaces().size() > 0);

        Space space = spaces.getSpaces().get(0);
        link = getFirstLinkByRelation(space, Relations.PAGES);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Pages pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(pages.getPageSummaries().size() > 0);

        PageSummary pageSummary = pages.getPageSummaries().get(0);
        link = getFirstLinkByRelation(pageSummary, Relations.PAGE);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        return page;
    }

    @Override
    public void testRepresentation() throws Exception
    {
        Page page = getFirstPage();

        Link link = getFirstLinkByRelation(page, Relations.SELF);
        assertNotNull(link);

        checkLinks(page);
    }

    public void testGETNotExistingPage() throws Exception
    {
        GetMethod getMethod =
            executeGet(getUriBuilder(PageResource.class).build(getWiki(), "NOTEXISTING", "NOTEXISTING").toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode());

    }

    public void testPUTPage() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());
        final String TITLE = String.format("Title (%s)", UUID.randomUUID().toString());

        Page originalPage = getFirstPage();

        Page newPage = objectFactory.createPage();
        newPage.setContent(CONTENT);
        newPage.setTitle(TITLE);

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        PutMethod putMethod = executePutXml(link.getHref(), newPage, "Admin", "admin");
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getStatusCode());

        Page modifiedPage = (Page) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(modifiedPage.getContent(), CONTENT);
        assertEquals(modifiedPage.getTitle(), TITLE);
    }

    public void testPUTPageWithTextPlain() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        PutMethod putMethod = executePut(link.getHref(), CONTENT, MediaType.TEXT_PLAIN, "Admin", "admin");
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getStatusCode());

        Page modifiedPage = (Page) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(modifiedPage.getContent(), CONTENT);
    }

    public void testPUTPageUnauthorized() throws Exception
    {
        Page page = getFirstPage();
        page.setContent("New content");

        Link link = getFirstLinkByRelation(page, Relations.SELF);
        assertNotNull(link);

        PutMethod putMethod = executePutXml(link.getHref(), page);
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_UNAUTHORIZED, putMethod.getStatusCode());
    }

    public void testPUTNonExistingPage() throws Exception
    {
        final String SPACE_NAME = "Test";
        final String PAGE_NAME = String.format("Test-%d", System.currentTimeMillis());
        final String CONTENT = String.format("Content %d", System.currentTimeMillis());
        final String TITLE = String.format("Title %d", System.currentTimeMillis());
        final String PARENT = "Main.WebHome";

        Page page = objectFactory.createPage();
        page.setContent(CONTENT);
        page.setTitle(TITLE);
        page.setParent(PARENT);

        PutMethod putMethod =
            executePutXml(getUriBuilder(PageResource.class).build(getWiki(), SPACE_NAME, PAGE_NAME).toString(), page,
                "Admin", "admin");
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_CREATED, putMethod.getStatusCode());

        Page modifiedPage = (Page) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(CONTENT, modifiedPage.getContent());
        assertEquals(TITLE, modifiedPage.getTitle());
        assertEquals(PARENT, modifiedPage.getParent());
    }

    public void testPUTWithInvalidRepresentation() throws Exception
    {
        Page page = getFirstPage();
        Link link = getFirstLinkByRelation(page, Relations.SELF);

        PutMethod putMethod =
            executePut(link.getHref(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><invalidPage><content/></invalidPage>", MediaType.TEXT_XML);
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_BAD_REQUEST, putMethod.getStatusCode());

    }

    private void createPageIfDoesntExist(String spaceName, String pageName, String content) throws Exception
    {
        String uri = getUriBuilder(PageResource.class).build(getWiki(), spaceName, pageName).toString();

        GetMethod getMethod = executeGet(uri);

        if (getMethod.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            Page page = objectFactory.createPage();
            page.setContent(content);

            PutMethod putMethod = executePutXml(uri, page, "Admin", "admin");
            assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_CREATED, putMethod.getStatusCode());

            getMethod = executeGet(uri);
            assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
        }
    }

    public void testPUTTranslation() throws Exception
    {
        final String languageId = String.format("%d", random.nextLong());

        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME, "Translations");

        Page page = objectFactory.createPage();
        page.setContent(languageId);

        PutMethod putMethod =
            executePutXml(getUriBuilder(PageTranslationResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME, languageId).toString(), page, "Admin", "admin");
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_CREATED, putMethod.getStatusCode());

        GetMethod getMethod =
            executeGet(getUriBuilder(PageTranslationResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME, languageId).toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page modifiedPage = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertEquals(languageId, modifiedPage.getLanguage());
        assertEquals(languageId, modifiedPage.getLanguage());
    }

    public void testGETTranslations() throws Exception
    {
        GetMethod getMethod =
            executeGet(getUriBuilder(PageResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME).toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertTrue(page.getTranslations().getTranslations().size() > 0);

        for (Translation translation : page.getTranslations().getTranslations()) {
            getMethod = executeGet(getFirstLinkByRelation(translation, Relations.PAGE).getHref());
            assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

            page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            assertEquals(page.getLanguage(), translation.getLanguage());

            checkLinks(translation);
        }
    }

    public void testGETNotExistingTranslation() throws Exception
    {
        GetMethod getMethod =
            executeGet(getUriBuilder(PageResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME).toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        getMethod =
            executeGet(getUriBuilder(PageTranslationResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME, "NOT_EXISTING").toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode());

    }

    public void testDELETEPage() throws Exception
    {
        final String pageName = String.format("Test-%d", random.nextLong());

        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, pageName, "Test page");

        DeleteMethod deleteMethod =
            executeDelete(getUriBuilder(PageResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME, pageName)
                .toString(), "Admin", "admin");
        assertEquals(getHttpMethodInfo(deleteMethod), HttpStatus.SC_NO_CONTENT, deleteMethod.getStatusCode());

        GetMethod getMethod =
            executeGet(getUriBuilder(PageResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME, pageName)
                .toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode());
    }

    public void testDELETEPageNoRights() throws Exception
    {
        final String pageName = String.format("Test-%d", random.nextLong());

        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, pageName, "Test page");

        DeleteMethod deleteMethod =
            executeDelete(getUriBuilder(PageResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME, pageName)
                .toString());
        assertEquals(getHttpMethodInfo(deleteMethod), HttpStatus.SC_UNAUTHORIZED, deleteMethod.getStatusCode());

        GetMethod getMethod =
            executeGet(getUriBuilder(PageResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME, pageName)
                .toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
    }

    public void testPageHistory() throws Exception
    {
        GetMethod getMethod =
            executeGet(getUriBuilder(PageResource.class).build(getWiki(), "Main", "WebHome").toString());

        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page originalPage = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        String pageHistoryUri =
            getUriBuilder(PageHistoryResource.class).build(getWiki(), originalPage.getSpace(), originalPage.getName())
                .toString();

        getMethod = executeGet(pageHistoryUri);
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        History history = (History) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        for (HistorySummary historySummary : history.getHistorySummaries()) {
            getMethod = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

            Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            checkLinks(page);

            for (Translation translation : page.getTranslations().getTranslations()) {
                checkLinks(translation);
            }
        }
    }

    public void testPageTranslationHistory() throws Exception
    {
        String pageHistoryUri =
            getUriBuilder(PageHistoryResource.class).build(getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME).toString();

        GetMethod getMethod = executeGet(pageHistoryUri);
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        History history = (History) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        for (HistorySummary historySummary : history.getHistorySummaries()) {
            getMethod = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

            Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            checkLinks(page);
            checkLinks(page.getTranslations());
        }
    }

    public void testGETPageChildren() throws Exception
    {
        GetMethod getMethod =
            executeGet(getUriBuilder(PageChildrenResource.class).build(getWiki(), "Main", "WebHome").toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Pages pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(pages.getPageSummaries().size() > 0);

        for (PageSummary pageSummary : pages.getPageSummaries()) {
            checkLinks(pageSummary);
        }
    }

    public void testPOSTPageFormUrlEncoded() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());
        final String TITLE = String.format("Title (%s)", UUID.randomUUID().toString());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        NameValuePair[] nameValuePairs = new NameValuePair[2];
        nameValuePairs[0] = new NameValuePair("title", TITLE);
        nameValuePairs[1] = new NameValuePair("content", CONTENT);

        PostMethod postMethod =
            executePostForm(String.format("%s?method=PUT", link.getHref()), nameValuePairs, "Admin", "admin");
        assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_ACCEPTED, postMethod.getStatusCode());

        Page modifiedPage = (Page) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        assertEquals(modifiedPage.getContent(), CONTENT);
        assertEquals(modifiedPage.getTitle(), TITLE);
    }

    public void testPUTPageSyntax() throws Exception
    {
        Page originalPage = getFirstPage();

        GetMethod getMethod = executeGet(getFullUri(SyntaxesResource.class));
        Syntaxes syntaxes = (Syntaxes) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        String newSyntax = null;
        for (String syntax : syntaxes.getSyntaxes()) {
            if (!syntax.equals(originalPage.getSyntax())) {
                newSyntax = syntax;
                break;
            }
        }

        originalPage.setSyntax(newSyntax);

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        PutMethod putMethod = executePutXml(link.getHref(), originalPage, "Admin", "admin");
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getStatusCode());

        Page modifiedPage = (Page) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(newSyntax, modifiedPage.getSyntax());
    }
}
