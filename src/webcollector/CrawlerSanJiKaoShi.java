/*
 * Copyright (C) 2015 hu
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
package webcollector;

import java.util.ArrayList;
import java.util.Scanner;


import org.json.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.jdbc.core.JdbcTemplate;

import dao.JDBCHelper;
import model.CrawlDatum;
import model.CrawlDatums;
import model.Page;
import net.HttpRequest;
import net.HttpResponse;
import plugin.berkeley.BreadthCrawler;


/**
 * WebCollector 2.x版本的tutorial(2.20以上) 
 * 2.x版本特性：
 * 1）自定义遍历策略，可完成更为复杂的遍历业务，例如分页、AJAX
 * 2）可以为每个URL设置附加信息(MetaData)，利用附加信息可以完成很多复杂业务，例如深度获取、锚文本获取、引用页面获取、POST参数传递、增量更新等。
 * 3）使用插件机制，WebCollector内置两套插件。
 * 4）内置一套基于内存的插件（RamCrawler)，不依赖文件系统或数据库，适合一次性爬取，例如实时爬取搜索引擎。
 * 5）内置一套基于Berkeley DB（BreadthCrawler)的插件：适合处理长期和大量级的任务，并具有断点爬取功能，不会因为宕机、关闭导致数据丢失。 
 * 6）集成selenium，可以对javascript生成信息进行抽取
 * 7）可轻松自定义http请求，并内置多代理随机切换功能。 可通过定义http请求实现模拟登录。 
 * 8）使用slf4j作为日志门面，可对接多种日志
 *
 * 可在webcollector包中找到例子(Demo)
 *
 * @author hu
 */
public class CrawlerSanJiKaoShi extends BreadthCrawler {

    /*
        该例子利用正则控制爬虫的遍历，
        另一种常用遍历方法可参考DemoTypeCrawler
    */
    
    public CrawlerSanJiKaoShi(String crawlPath, boolean autoParse) {
        super(crawlPath, autoParse);

        //需要抓取图片时设置为true，并加入图片的正则规则
        //setParseImg(true);
        
        //设置每个线程的抓取间隔（毫秒）
        setExecuteInterval(1000);
        
        //设置线程数
        setThreads(20);
    }

    private static JdbcTemplate jdbcTemplate = null;
    
    
    /*
        可以往next中添加希望后续爬取的任务，任务可以是URL或者CrawlDatum
        爬虫不会重复爬取任务，从2.20版之后，爬虫根据CrawlDatum的key去重，而不是URL
        因此如果希望重复爬取某个URL，只要将CrawlDatum的key设置为一个历史中不存在的值即可
        例如增量爬取，可以使用 爬取时间+URL作为key。
    
        新版本中，可以直接通过 page.select(css选择器)方法来抽取网页中的信息，等价于
        page.getDoc().select(css选择器)方法，page.getDoc()获取到的是Jsoup中的
        Document对象，细节请参考Jsoup教程
    */
    @Override
    public HttpResponse getResponse(CrawlDatum crawlDatum) throws Exception {
        HttpRequest request = new HttpRequest(crawlDatum.url());
        //设置链接超时
        request.setTimeoutForConnect(90000);
        request.setTimeoutForRead(90000);
        request.setMethod("GET");
        String outputData = crawlDatum.meta("outputData");
        if (outputData != null) {
            request.setOutputData(outputData.getBytes("utf-8"));
        }
        
        
        //通过下面方式可以设置Cookie、User-Agent等http请求头信息
        
        StringBuilder sb = new StringBuilder();
        //sb.append("AD_RS_COOKIE" + "=" + "20080920" + ";");
        sb.append("JSESSIONID" + "=" + "416B374BDA976D9DB04E5A7718685A25" + ";");
        
        String result = sb.toString();
        
        request.setCookie(result);
        request.setUserAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36");
        //request.addHeader("xxx", "xxxxxxxxx");
        
        return request.response();
    }
    
    @Override
    public void visit(Page page, CrawlDatums next) {
        if (page.matchUrl("http://10.168.15.14/query/list/.*")) {
        	//ArrayList<String> title = page.attrs("table[data-fixed-table] td","text()");
            Elements elements = page.select("table[data-fixed-table] td");
            //String author = page.select("div[id=blog_userface]").first().text();
            int m=1;
            int wId=0;
            String wName="";
            String wSex="";
            String wSenFenZ="";
            String wStation="";
            for(Element i : elements){
            	switch (m) {
				case 1:
					wId = Integer.parseInt(i.text());
					m++;
					break;
				case 2:
					wName = i.text();
					m++;
					break;
				case 3:
					wSex = i.text();
					m++;
					break;
				case 4:
					wSenFenZ = i.text();
					m++;
					break;
				case 5:
					wStation = i.text();
					m=1;
					
					//写入数据库
					if (jdbcTemplate != null) {
						int updates=jdbcTemplate.update("insert into jkdaxx"
								+" (wId,wName,wSex,wSenFenZ,wStation) value(?,?,?,?,?)",
								wId,wName,wSex,wSenFenZ,wStation);
						if(updates==1){
							System.out.println("mysql插入成功");
						}
					}
					
					break;
				default:
					break;
				}
            	
            	System.out.println("title:" + i.text() + ";\r");
            	
            }
            System.out.println("visiting:"+page.url()+"\tdepth="+page.meta("depth"));
        	
        }
    }
    @Override
    protected void afterParse(Page page, CrawlDatums next) {
  //当前页面的depth为x，则从当前页面解析的后续任务的depth为x+1
        int depth;
        //如果在添加种子时忘记添加depth信息，可以通过这种方式保证程序不出错
        if(page.meta("depth")==null){
            depth=1;
        }else{
            depth=Integer.valueOf(page.meta("depth"));
        }
        depth++;
        for(CrawlDatum datum:next){
            datum.meta("depth", depth+"");
        }
    }
    

    public static void main(String[] args) throws Exception {
    	//连接数据库
    	Scanner sc = new Scanner(System.in);
    	
    	try {
    		
    	    jdbcTemplate = JDBCHelper.createMysqlTemplate("mysql1",
    	            "jdbc:mysql://127.0.0.1:3306/webcollector?useUnicode=true&characterEncoding=utf8",
    	            "msadmin", "123456", 5, 30);

    	    /*创建数据表*/
    	    jdbcTemplate.execute("CREATE TABLE `jkdaxx` (   `wId` int(11) NOT NULL COMMENT '序号',   `wName` varchar(20) DEFAULT NULL COMMENT '姓名',   `wSex` varchar(10) DEFAULT NULL,   `wSenFenZ` varchar(30) DEFAULT NULL,   `wStation` varchar(255) DEFAULT NULL,   PRIMARY KEY (`wId`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
    	    System.out.println("成功创建数据表 jkdaxx");
    	} catch (Exception ex) {
    	    jdbcTemplate = null;
    	    System.out.println("mysql未开启或JDBCHelper.createMysqlTemplate中参数配置不正确!");
    	}
    	
    	
    	//System.out.println("设置执行间隔（1000=1s）：");
    	//Long jiange = sc.nextLong();
    	//System.out.println("设置迭代次数：");
    	//int diedai = sc.nextInt();
        //TutorialCrawler crawler = new TutorialCrawler("crawl", true);
        //crawler.setExecuteInterval(jiange);
    	//crawler.start(2);

    	String strBefore = "http://10.168.15.14/query/list/0101?pageSize=10000&pageNumber=";
    	String strAfter = "&birthdayBegin=&birthdayEnd=&createTimeBegin=&createTimeEnd=&death=0&deathTimeBegin=&deathTimeEnd=&livingDivision=440605124&managementOrganization=440605124&userSelectedOrderQueryColumnIds=0101016&userSelectedQueryColumnIds=0101001,0101002,0101003,0101019";
        CrawlerSanJiKaoShi crawler = new CrawlerSanJiKaoShi("crawl", true);
        crawler.addSeed("http://10.168.15.14/query/list/0101?pageSize=10000&pageNumber=0&birthdayBegin=&birthdayEnd=&createTimeBegin=&createTimeEnd=&death=0&deathTimeBegin=&deathTimeEnd=&livingDivision=440605124&managementOrganization=440605124&userSelectedOrderQueryColumnIds=0101016&userSelectedQueryColumnIds=0101001,0101002,0101003,0101019");
        System.out.println("设置最后页数：");
        int pagescount = sc.nextInt();
        for(int i=1;i<=pagescount;i++){
            crawler.addSeed(new CrawlDatum(strBefore+i+strAfter)
                    .meta("depth", "1"));
        }
        System.out.println("增加的种子数："+crawler.seeds.size());
//        for(int i=0;i<crawler.seeds.size();i++){
//        	System.out.println("种子"+i+":"+crawler.seeds.get(i).url());
//        }
        
        /*正则规则用于控制爬虫自动解析出的链接，用户手动添加的链接，例如添加的种子、或
          在visit方法中添加到next中的链接并不会参与正则过滤*/
        /*自动爬取类似"http://news.hfut.edu.cn/show-xxxxxxhtml"的链接*/
        //addRegex("http://blog.csdn.net/.*/article/details/.*");
        crawler.addRegex("http://10.168.15.14/query/list/.*");        
        //crawler.addRegex("http://news.hfut.edu.cn/show-.*html");
        /*不要爬取jpg|png|gif*/
        //crawler.addRegex("-.*\\.(jpg|png|gif).*");
        /*不要爬取包含"#"的链接*/
        //crawler.addRegex("-.*#.*");
        crawler.setTopN(pagescount+1);

        System.out.println("设置迭代次数：");
        int dd = sc.nextInt();
        sc.close();
        crawler.start(dd);
    }

}
