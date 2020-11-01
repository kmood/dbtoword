package com.dbexport.dbtoword;

import com.dbexport.util.JdbcUtil;
import com.dbexport.util.JsonBinder;
import com.google.gson.Gson;
import com.kmood.datahandle.DocumentProducer;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletContext;
import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: SunBC
 * @Date: 2019/10/16 15:59
 * @Description:
 */
@Controller
public class Export {
    @Autowired
    private ServletContext servletContext;
    private static final Gson jsonBinder = JsonBinder.buildNormalBinder("yyyy-MM-dd HH:mm:ss");
    @RequestMapping(path = "/ExportDBPost")
    public ResponseEntity<byte[]> ExportDB(String db_type, String db_user, String db_pwd, String db_host, String db_port, String db_name) throws Exception {

        //获得要下载的File对象
        File file = new File(servletContext.getRealPath(generatorDBWord_mysql(db_user, db_pwd, db_host, db_port, db_name)));
        //创建springframework的HttpHeaders对象
        HttpHeaders headers = new HttpHeaders();
        //下载显示的文件名,解决文件名称乱码问题
        String downLoadFileName = new String(file.getName().getBytes("UTF-8"), "iso-8859-1");
        //通知浏览器以attachment（下载方式）打开文件
        headers.setContentDispositionFormData("attachment", downLoadFileName);
        //application/octet-stream:二进制流数据(最常见的文件下载)
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        //返回状态码：201 HttpStatus.CREATED :请求已经被实现，而且有一个新的资源已经依据请求的需要而建立，且其 URI 已经随Location 头信息返回

        return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(file),headers, HttpStatus.CREATED);
    }

    @RequestMapping(path = "/ExportDBGet")
    public String ExportDBGet( String db_type, String db_user, String db_pwd, String db_host, String db_port, String db_name) throws Exception {
        return generatorDBWord_mysql(db_user, db_pwd, db_host, db_port, db_name);
    }
    public String generatorDBWord_pg(String db_user, String db_pwd, String db_host, String db_port, String db_name,String schama) throws Exception {
        String s;
        Connection pgsqlConnect = null;
        try {
            String sql = "SELECT tablename TABLE_NAME,obj_description(relfilenode,'pg_class') TABLE_COMMENT FROM pg_tables a,pg_class b WHERE a.tablename = b.relname and a.tablename NOT LIKE 'pg%' and a.tablename NOT LIKE '%_fc' and a.tablename NOT LIKE '%_temp' and a.tablename NOT LIKE '%_pc' AND a.tablename NOT LIKE 'sql_%' AND obj_description(relfilenode,'pg_class') is not null   ORDER BY a.tablename";
            pgsqlConnect = JdbcUtil.getPgsqlConnect(db_user, db_pwd, db_host, db_port, db_name,schama);
            QueryRunner queryRunner = new QueryRunner();
            HashMap<String, Object> exportMap = new HashMap<>();
            List<Map<String, Object>> tableList = queryRunner.query(pgsqlConnect, sql, new MapListHandler());
            int i = 1;
            for (Map map : tableList) {
                Object table_name = map.get("TABLE_NAME");
                String sql_table = "SELECT a.attname AS \"column_name\",\n" +
                        "            pg_catalog.format_type(a.atttypid, a.atttypmod) as \"column_type\",\n" +
                        "            CASE WHEN a.attnotnull IS TRUE\n" +
                        "              THEN 'NO'\n" +
                        "              ELSE 'YES'\n" +
                        "            END AS \"null\",\n" +
                        "            CASE WHEN pg_catalog.pg_get_expr(adef.adbin, adef.adrelid, true) IS NOT NULL\n" +
                        "              THEN pg_catalog.pg_get_expr(adef.adbin, adef.adrelid, true)\n" +
                        "            END as \"Default\",\n" +
                        "            CASE WHEN pg_catalog.col_description(a.attrelid, a.attnum) IS NULL\n" +
                        "            THEN ''\n" +
                        "            ELSE pg_catalog.col_description(a.attrelid, a.attnum)\n" +
                        "            END  AS \"column_comment\"\n" +
                        "          FROM pg_catalog.pg_attribute a\n" +
                        "          LEFT JOIN pg_catalog.pg_attrdef adef ON a.attrelid=adef.adrelid AND a.attnum=adef.adnum\n" +
                        "          LEFT JOIN pg_catalog.pg_type t ON a.atttypid=t.oid\n" +
                        "          WHERE a.attrelid =\n" +
                        "            (SELECT oid FROM pg_catalog.pg_class WHERE relname='"+table_name+"'\n" +
                        "              AND relnamespace = (SELECT oid FROM pg_catalog.pg_namespace WHERE\n" +
                        "              nspname = 'public')\n" +
                        "            )\n" +
                        "          AND a.attnum > 0 AND NOT a.attisdropped\n" +
                        "          ORDER BY a.attnum";
                List<Map<String, Object>> columns = queryRunner.query(pgsqlConnect, sql_table, new MapListHandler());
                map.put("columns",columns);
                map.put("index",i++);
            }
            exportMap.put("tables",tableList);
            String modelpath = servletContext.getRealPath("/WEB-INF/classes/model/");
            s = "/resources/" + db_name + System.currentTimeMillis() + ".doc";
            String generatorPath = servletContext.getRealPath(s);
//            String modelpath = "C:\\Users\\admin\\Desktop\\";
//            s = "/resources/" + db_name + System.currentTimeMillis() + ".doc";
//            String generatorPath = "C:\\Users\\admin\\Desktop\\";
            DocumentProducer dp = new DocumentProducer(modelpath);
            String complie = dp.Complie(modelpath, "DbExportModel.xml", true);
            System.out.println(jsonBinder.toJson(exportMap));
            dp.produce(exportMap, generatorPath);
        } finally {
            JdbcUtil.close(pgsqlConnect,null,null);
        }
        return s;
    }
    private String generatorDBWord_mysql(String db_user, String db_pwd, String db_host, String db_port, String db_name) throws Exception {
        String s;
        Connection mysqlConnect = null;
        try {
            String sql = "select t.TABLE_NAME,t.TABLE_COMMENT from information_schema.`TABLES` t where TABLE_SCHEMA='"+db_name+"' AND t.TABLE_TYPE !='VIEW'";
            mysqlConnect = JdbcUtil.getMysqlConnect(db_user, db_pwd, db_host, db_port, db_name);
            QueryRunner queryRunner = new QueryRunner();
            HashMap<String, Object> exportMap = new HashMap<>();
            List<Map<String, Object>> tableList = queryRunner.query(mysqlConnect, sql, new MapListHandler());
            int i = 1;
            for (Map map : tableList) {
                Object table_name = map.get("TABLE_NAME");
                String sql_table = "select column_name,column_type,column_comment from information_schema.columns  where  TABLE_SCHEMA='"+db_name+"' and table_name='"+table_name+"'";
                List<Map<String, Object>> columns = queryRunner.query(mysqlConnect, sql_table, new MapListHandler());
                map.put("columns",columns);
                map.put("index",i++);
            }
            exportMap.put("tables",tableList);
            String modelpath = servletContext.getRealPath("/WEB-INF/classes/model/");
            s = "/resources/" + db_name + System.currentTimeMillis() + ".doc";
            String generatorPath = servletContext.getRealPath(s);

            DocumentProducer dp = new DocumentProducer(modelpath);
            String complie = dp.Complie(modelpath, "DbExportModel.xml", true);
            System.out.println(jsonBinder.toJson(exportMap));
            dp.produce(exportMap, generatorPath);
        } finally {
            JdbcUtil.close(mysqlConnect,null,null);
        }
        return s;
    }
}
