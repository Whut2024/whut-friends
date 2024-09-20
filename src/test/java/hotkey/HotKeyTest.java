package hotkey;

import com.jd.platform.hotkey.client.ClientStarter;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import org.springframework.context.annotation.Bean;

/**
 * @author whut2024
 * @since 2024-09-20
 */
public class HotKeyTest {

    private static class HotKeyConfig {

        /**
         * Etcd 服务器完整地址
         */
        private String etcdServer = "http://123.60.168.252:2379";

        /**
         * 应用名称
         * 这个参数必须于 规则所属App名字一致
         */
        private String appName = "whut-friends";

        /**
         * 本地缓存最大数量
         */
        private int caffeineSize = 10000;

        /**
         * 批量推送 key 的间隔时间
         */
        private long pushPeriod = 1000L;

        /**
         * 初始化 hotkey
         */
        public void initHotkey() {
            ClientStarter.Builder builder = new ClientStarter.Builder();
            ClientStarter starter = builder.setAppName(appName)
                    .setCaffeineSize(caffeineSize)
                    .setPushPeriod(pushPeriod)
                    .setEtcdServer(etcdServer)
                    .build();
            starter.startPipeline();
        }
    }


    public static void main(String[] args) {

        new Thread(() -> {

            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            final String testKey = "hot_test_1";

            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (JdHotKeyStore.isHotKey(testKey)) {
                    final Object cacheObject = JdHotKeyStore.get(testKey);

                    if (cacheObject != null) {
                        System.out.println(i + ":是热点，获得缓存");
                        continue;
                    }

                    System.out.println(i + ":是热点，但还没有缓存");
                    JdHotKeyStore.smartSet(testKey, "cache");
                    continue;
                }

                System.out.println(i + ":不是热点");
            }


        }).start();

        new HotKeyConfig().initHotkey();


    }
}
