/*
 * Copyright (C) 2014 hu
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package crawler;

import fetcher.Executor;
import fetcher.Visitor;
import model.CrawlDatum;
import model.CrawlDatums;
import model.Links;
import model.Page;
import net.HttpRequest;
import net.HttpResponse;
import net.Requester;
import util.RegexRule;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hu
 */
public abstract class AutoParseCrawler extends Crawler implements Executor, Visitor, Requester {

    public static final Logger LOG = LoggerFactory.getLogger(AutoParseCrawler.class);

    /**
     * 是否自动抽取符合正则的链接并加入后续任务
     */
    protected boolean autoParse = true;
    protected boolean parseImg = false;

    protected Visitor visitor;
    protected Requester requester;

    public AutoParseCrawler(boolean autoParse) {
        this.autoParse = autoParse;
        this.visitor = this;
        this.requester = this;
        this.executor = this;
    }

    @Override
    public HttpResponse getResponse(CrawlDatum crawlDatum) throws Exception {
        HttpRequest request = new HttpRequest(crawlDatum);
        return request.response();
    }

    /**
     * URL正则约束
     */
    protected RegexRule regexRule = new RegexRule();

    @Override
    public void execute(CrawlDatum datum, CrawlDatums next) throws Exception {
        HttpResponse response = requester.getResponse(datum);
        Page page = new Page(datum, response);
        visitor.visit(page, next);
        if (autoParse && !regexRule.isEmpty()) {
            parseLink(page, next);
        }
        afterParse(page, next);
    }

    protected void afterParse(Page page, CrawlDatums next) {

    }

    protected void parseLink(Page page, CrawlDatums next) {
        String conteType = page.contentType();
        if (conteType != null && conteType.contains("text/html")) {
            Document doc = page.doc();
            if (doc != null) {
                Links links = new Links().addByRegex(doc, regexRule,parseImg);
                next.add(links);
            }
        }

    }

    /**
     * 添加URL正则约束
     *
     * @param urlRegex URL正则约束
     */
    public void addRegex(String urlRegex) {
        regexRule.addRule(urlRegex);
    }

    /**
     *
     * @return 返回是否自动抽取符合正则的链接并加入后续任务
     */
    public boolean isAutoParse() {
        return autoParse;
    }

    /**
     * 设置是否自动抽取符合正则的链接并加入后续任务
     *
     * @param autoParse 是否自动抽取符合正则的链接并加入后续任务
     */
    public void setAutoParse(boolean autoParse) {
        this.autoParse = autoParse;
    }

    /**
     * 获取正则规则
     *
     * @return 正则规则
     */
    public RegexRule getRegexRule() {
        return regexRule;
    }

    /**
     * 设置正则规则
     *
     * @param regexRule 正则规则
     */
    public void setRegexRule(RegexRule regexRule) {
        this.regexRule = regexRule;
    }

    /**
     * 获取Visitor
     *
     * @return Visitor
     */
    public Visitor getVisitor() {
        return visitor;
    }

    /**
     * 设置Visitor
     *
     * @param visitor Visitor
     */
    public void setVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public Requester getRequester() {
        return requester;
    }

    public void setRequester(Requester requester) {
        this.requester = requester;
    }

    public boolean isParseImg() {
        return parseImg;
    }

    public void setParseImg(boolean parseImg) {
        this.parseImg = parseImg;
    }
    
    
}
