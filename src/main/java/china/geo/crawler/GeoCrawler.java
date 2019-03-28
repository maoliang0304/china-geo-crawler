package china.geo.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 从国家统计局官方网站（http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/）抓取省市区三级数据数据，并导出txt.
 */
public class GeoCrawler {

    private static final String[] UAS = {
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 BIDUBrowser/8.3 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36 Core/1.47.277.400 QQBrowser/9.4.7658.400",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 UBrowser/5.6.12150.8 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.122 Safari/537.36 SE 2.X MetaSr 1.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36 TheWorld 7",
            "Mozilla/5.0 (Windows NT 6.1; W…) Gecko/20100101 Firefox/60.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36 OPR/37.0.2178.32",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko",
            "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)",
            "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)",
            "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0)",};

    private static int NUM = new Random().nextInt(UAS.length - 1);

    private static String getUserAgent() {
        if (NUM == UAS.length) {
            NUM = 0;
        }
        return UAS[NUM++];
    }

    /**
     * 如果出现（Unexpected end of file from server）异常，可以适当将Thread.sleep改大.
     */
    public static Document httpGet(String url) throws IOException {
        Connection con = Jsoup.connect(url);
        con.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        con.header("Accept-Encoding", "gzip, deflate");
        con.header("Accept-Language", "zh-CN,zh;q=0.9");
        con.header("Cache-Control", "max-age=0");
        con.header("Connection", "keep-alive");
        con.header("Host", "www.stats.gov.cn");
        String ua = getUserAgent();
        System.out.println(ua);
        con.header("User-Agent", ua);
        con.timeout(1000);
        Document doc = con.get();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        return doc;
    }

    public static List<Geo> getProvinceList(String year) throws IOException {
        String url = "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/" + year + "/index.html";
        System.out.println(url);
        Document document = httpGet(url);
        Elements trs = document.body().getElementsByAttributeValue("class", "provincetr");
        List<Geo> provinces = new ArrayList<Geo>();
        for (Element tr : trs) {
            Elements tds = tr.children();
            for (Element td : tds) {
                String urrCode = td.select("a").attr("href").replace(".html", "");
                String name = td.select("a").toString().replace("<a href=\"" + urrCode + ".html\">", "").replace("<br></a>", "");
                Integer code = Integer.parseInt(urrCode) * 10000;
                provinces.add(new Geo().setLevel(1).setParentCode(0).setCode(code).setName(name));
            }
        }
        return provinces;
    }

    private static List<Geo> getCityList(String year, Integer provinceCode) throws IOException {
        String urrProvinceCode = Integer.toString(provinceCode / 10000);
        String url = "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/" + year + "/" + urrProvinceCode + ".html";
        System.out.println(url);
        Document document = httpGet(url);
        Elements trs = document.body().getElementsByAttributeValue("class", "citytr");
        List<Geo> citys = new ArrayList<Geo>();
        for (Element tr : trs) {
            Elements tds = tr.children();
            Element td = tds.get(1);
            String urrCode = td.select("a").attr("href").replace(urrProvinceCode + "/", "").replace(".html", "");
            String name = td.select("a").toString().replace("<a href=\"" + urrProvinceCode + "/" + urrCode + ".html\">", "").replace("</a>", "");
            Integer code = Integer.parseInt(urrCode) * 100;
            citys.add(new Geo().setLevel(2).setParentCode(provinceCode).setCode(code).setName(name));
        }
        return citys;
    }

    private static List<Geo> getAreaList(String year, Integer provinceCode, Integer cityCode) throws IOException {
        String urrProvinceCode = Integer.toString(provinceCode / 10000);
        String urrCityCode = Integer.toString(cityCode / 100);
        String url = "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/" + year + "/" + urrProvinceCode + "/" + urrCityCode + ".html";
        System.out.println(url);
        Document document = httpGet(url);
        Elements trs = document.body().getElementsByAttributeValue("class", "countytr");
        List<Geo> areas = new ArrayList<Geo>();
        for (Element tr : trs) {
            Elements tds = tr.children();
            Element codeTd = tds.get(0);
            Elements aCodeElements = codeTd.select("a");
            Integer code = Integer.parseInt(Long.toString(Long.parseLong(aCodeElements.size() > 0 ? aCodeElements.first().html() : codeTd.html()) / 1000000));
            Element nameTd = tds.get(1);
            Elements aNameElements = nameTd.select("a");
            String name = aNameElements.size() > 0 ? aNameElements.first().html() : nameTd.html();
            areas.add(new Geo().setLevel(3).setParentCode(cityCode).setCode(code).setName(name));
        }
        return areas;
    }

    /**
     * 创建文件
     */
    public static boolean createFile(File fileName) throws Exception {
        boolean flag = false;
        try {
            if (!fileName.exists()) {
                fileName.createNewFile();
                flag = true;
            } else {
                fileName.delete();
                fileName.createNewFile();
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean writeTxtFile(String content, File fileName) throws Exception {
        RandomAccessFile mm = null;
        boolean flag = false;
        FileOutputStream o = null;
        try {
            o = new FileOutputStream(fileName);
            o.write(content.getBytes("UTF-8"));
            o.close();
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mm != null) {
                mm.close();
            }
        }
        return flag;
    }


    public static void main(String[] args) throws Exception {
        String year = "2018";
        String exportPath = "d://china-geo.txt";
        List<Geo> provinces = getProvinceList(year);

        List<Geo> citys = new ArrayList<Geo>();
        for (int i = 0; i < provinces.size(); i++) {
            Geo province = provinces.get(i);
            citys.addAll(getCityList(year, province.getCode()));
        }

        List<Geo> areas = new ArrayList<Geo>();
        for (int i = 0; i < citys.size(); i++) {
            Geo city = citys.get(i);
            areas.addAll(getAreaList(year, city.getParentCode(), city.getCode()));
        }
        File file = new File(exportPath);
        createFile(file);

        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < provinces.size(); i++) {
            Geo province = provinces.get(i);
            stringBuffer.append(province.getLevel() + "\t" + province.getParentCode() + "\t" + province.getCode() + "\t" + province.getName() + "\n");
        }
        for (int i = 0; i < citys.size(); i++) {
            Geo city = citys.get(i);
            stringBuffer.append(city.getLevel() + "\t" + city.getParentCode() + "\t" + city.getCode() + "\t" + city.getName() + "\n");
        }
        for (int i = 0; i < areas.size(); i++) {
            Geo area = areas.get(i);
            stringBuffer.append(area.getLevel() + "\t" + area.getParentCode() + "\t" + area.getCode() + "\t" + area.getName() + "\n");
        }
        writeTxtFile(stringBuffer.toString(), file);
    }
}
