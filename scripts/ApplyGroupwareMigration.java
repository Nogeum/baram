import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

// Additive, resumable migration. Credentials are read from the local configuration,
// never included in arguments or logs. Existing business rows are not modified.
public class ApplyGroupwareMigration {
    public static void main(String[] args) throws Exception {
        var config=new Properties();
        try(var reader=Files.newBufferedReader(Path.of("config/application-oracle-local.properties"),StandardCharsets.UTF_8)){config.load(reader);}
        var credentials=new Properties();
        credentials.setProperty("user",config.getProperty("spring.datasource.username"));
        credentials.setProperty("password",config.getProperty("spring.datasource.password"));
        credentials.setProperty("oracle.jdbc.timezoneAsRegion","false");
        String filename=args.length==0?"20260924_groupware.sql":args[0];
        if(!Set.of("20260924_groupware.sql","20260924_reservation_review.sql","20260924_chat.sql","20260924_profile_image.sql").contains(filename))throw new IllegalArgumentException("Unknown migration");
        String source=Files.readString(Path.of("src/main/resources/db/oracle/migrations",filename),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","");
        var statements=Arrays.stream(source.split(";")).map(String::trim).filter(s->!s.isEmpty()).toList();
        for(String sql:statements)if(!sql.toLowerCase(Locale.ROOT).matches("(?s)(create table portal_.*|create index ix_.*|alter table portal_.* add .*)"))throw new IllegalStateException("Non-additive SQL refused");
        try(var connection=DriverManager.getConnection(config.getProperty("spring.datasource.url"),credentials)){
            int applied=0,existing=0;
            for(String sql:statements){
                try(var statement=connection.createStatement()){statement.execute(sql);applied++;}
                catch(SQLException ex){
                    if(Set.of(955,1430,2264,2275).contains(ex.getErrorCode()))existing++;
                    else throw new IllegalStateException("Migration failed with Oracle code "+ex.getErrorCode()+"; no destructive recovery attempted.",ex);
                }
            }
            System.out.println("Additive schema upgrade: applied="+applied+", already present="+existing);
            try(var statement=connection.createStatement();var rows=statement.executeQuery("select count(*) from user_tables where table_name like 'PORTAL_%'")){rows.next();System.out.println("Portal tables="+rows.getInt(1));}
        }
    }
}
