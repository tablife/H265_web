package gan.h265;

import android.os.Looper;
import gan.system.server.SystemServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(
		scanBasePackages = {"gan.h265.config","gan.h265.web"},
		exclude = DataSourceAutoConfiguration.class)
public class H265Server extends SystemServer {

	public static void main(String[] args) {
		Thread.currentThread().setName("main");
		Looper.prepareMainLooper();
		ApplicationContext context = new SpringApplicationBuilder(H265Server.class)
				.run(args);
		H265Server.getInstance().onCreate(context);
		Looper.loop();
	}

	public static H265Server getInstance() {
		return (H265Server) sInstance;
	}

	@Override
	protected void onCreate(ApplicationContext context) {
		super.onCreate(context);
	}

	public static String getPublicPath(String path){
		if(path.startsWith("/")){
			return getRootPath("/public"+path);
		}
		return getRootPath("/public/"+path);
	}

}
