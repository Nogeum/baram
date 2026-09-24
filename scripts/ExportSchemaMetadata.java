import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Reads only Oracle dictionary metadata. Never exports business rows or credentials. */
public class ExportSchemaMetadata {
    static String quote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";}
    static String query(Connection c,String sql)throws Exception{
        var rows=new ArrayList<String>();
        try(var st=c.createStatement();var rs=st.executeQuery(sql)){
            var md=rs.getMetaData();
            while(rs.next()){
                var fields=new ArrayList<String>();
                for(int i=1;i<=md.getColumnCount();i++){String value=rs.getString(i);fields.add(quote(md.getColumnLabel(i).toLowerCase(Locale.ROOT))+":"+(value==null?"null":quote(value)));}
                rows.add("{"+String.join(",",fields)+"}");
            }
        }
        return "["+String.join(",\n",rows)+"]";
    }
    public static void main(String[] args)throws Exception{
        var config=new Properties();try(var r=Files.newBufferedReader(Path.of("config/application-oracle-local.properties"),StandardCharsets.UTF_8)){config.load(r);}
        var auth=new Properties();auth.setProperty("user",config.getProperty("spring.datasource.username"));auth.setProperty("password",config.getProperty("spring.datasource.password"));auth.setProperty("oracle.jdbc.timezoneAsRegion","false");
        try(var c=DriverManager.getConnection(config.getProperty("spring.datasource.url"),auth)){
            var fields=new ArrayList<String>();
            fields.add("\"columns\":"+query(c,"select table_name,column_name,column_id,data_type,data_length,data_precision,data_scale,char_length,char_used,nullable,data_default from user_tab_columns where table_name like 'PORTAL\\_%' escape '\\' order by table_name,column_id"));
            fields.add("\"constraints\":"+query(c,"select c.table_name,c.constraint_name,c.constraint_type,c.status,c.validated,c.delete_rule,c.search_condition from user_constraints c where c.table_name like 'PORTAL\\_%' escape '\\' order by c.table_name,c.constraint_name"));
            fields.add("\"keys\":"+query(c,"select c.table_name,c.constraint_name,c.constraint_type,cc.column_name,cc.position,p.table_name target_table,pc.column_name target_column from user_constraints c join user_cons_columns cc on cc.constraint_name=c.constraint_name left join user_constraints p on p.constraint_name=c.r_constraint_name left join user_cons_columns pc on pc.constraint_name=p.constraint_name and pc.position=cc.position where c.constraint_type in ('P','U','R') and c.table_name like 'PORTAL\\_%' escape '\\' order by c.table_name,c.constraint_name,cc.position"));
            fields.add("\"indexes\":"+query(c,"select i.table_name,i.index_name,i.uniqueness,ic.column_name,ic.column_position from user_indexes i join user_ind_columns ic on ic.index_name=i.index_name where i.table_name like 'PORTAL\\_%' escape '\\' order by i.table_name,i.index_name,ic.column_position"));
            fields.add("\"sequences\":"+query(c,"select sequence_name,increment_by,min_value,max_value,cycle_flag,cache_size from user_sequences where sequence_name='PORTAL_SEQ'"));
            var path=Path.of("docs/database");Files.createDirectories(path);Files.writeString(path.resolve("schema-metadata.json"),"{\n"+String.join(",\n",fields)+"\n}",StandardCharsets.UTF_8);
            System.out.println("Oracle schema metadata exported; no business rows or credentials included.");
        }
    }
}
