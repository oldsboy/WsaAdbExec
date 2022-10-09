import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Scanner;

public class AdbExec {
    public static void main(String[] args) {
        String devices = execByRuntime("cmd /c adb devices -l");
        if (devices == null || !devices.startsWith("List of devices attached")) {
            System.out.println("估计是adb没安装或者adb的系统环境变量没有配置");
            return;
        }

        if (devices.substring("List of devices attached".length()).length() == 0) {     //  检测到无adb连接的情况,自动启动windows的android子系统
            System.out.println("未发现启动的usb设备,正在启动windows子系统");
            System.out.println(execByRuntime("cmd /c C:\\Users\\h\\AppData\\Local\\Microsoft\\WindowsApps\\MicrosoftCorporationII.WindowsSubsystemForAndroid_8wekyb3d8bbwe\\WsaClient.exe /launch wsa://com.amazon.venezia"));
            System.out.println(execByRuntime("cmd /c adb connect localhost:58526"));
        }

        select_device_flow();
    }

    private static void select_device_flow() {
        System.out.println("获取到设备列表:");
        String devices = execByRuntime("cmd /c adb devices -l");
        if (devices == null || !devices.startsWith("List of devices attached")) {
            System.out.println("这不可能发生!");
            return;
        }
        String device_source = devices.substring("List of devices attached".length());

        LinkedList<String> device_list = new LinkedList<>();
        String[] split = device_source.split("\\n");
        for (int i = 0; i < split.length; i++) {
            String device = split[i];
            device = device.trim();
            if (device.length() > 0) {
                String device_name = device.split("\\s{2,}")[0];
                String device_product = device.split("\\s{2,}")[1].replaceAll("device product:", "");
                device_list.add(device_name);
                System.out.printf("编号%s-设备名%s-设备信息:%s\n", device_list.size()-1, device_name, device_product);
            }
        }

        String dn = select_device(device_list);

        main_menu(dn);
    }

    /** @description    主菜单
     * @param device_name 设备名
     * @author oldsboy; @date 2022-10-09 16:47 */
    private static void main_menu(String device_name) {
        System.out.println("输入编号以执行命令:");
        System.out.println("0.重新选择设备");
        System.out.println("1.查看所有第三方app包名");
        System.out.println("2.查看设备信息");
        System.out.println("3.安装应用");
        System.out.println("c.执行自定义adb语句");
        System.out.println("q.退出");

        switch (new Scanner(System.in).next()) {
            case "0":
                select_device_flow();
                return;
            case "1":
                System.out.println(execByRuntime("adb -s " + device_name + " shell pm list packages -3"));
                break;
            case "2":
                showDeviceInfo(device_name);
                break;
            case "3":
                System.out.println("将apk拖到这里来,给出apk路径:");
                String path = new Scanner(System.in).nextLine();
                System.out.println("路径:"+path);
                break;
            case "c":
                System.out.println("请输入cmd语句:");
                System.out.println(execByRuntime(new Scanner(System.in).nextLine()));
                break;
            case "q":
                System.exit(0);
                return;
        }

        main_menu(device_name);
    }

    private static void showDeviceInfo(String device_name) {
        System.out.print("查看屏幕分辨率:");
        System.out.println(execByRuntime("adb -s " + device_name + " shell wm size"));
        System.out.print("查看屏幕密度:");
        System.out.println(execByRuntime("adb -s " + device_name + " shell wm density"));
        System.out.print("查看设备型号:");
        System.out.println(execByRuntime("adb -s " + device_name + " shell getprop ro.product.model"));
        System.out.print("获取设备序列号:");
        System.out.println(execByRuntime("adb -s " + device_name + " get-serialno"));
    }

    private static String select_device(LinkedList<String> device_list) {
        System.out.println("输入设备编号以选择:");
        String device_name = device_list.get(new Scanner(System.in).nextInt());
        if (device_name == null || device_name.length() == 0) {
            return select_device(device_list);
        }
        return device_name;
    }

    /**
     * 执行shell 命令， 命令中不必再带 adb shell
     * @param cmd
     * @return Sting  命令执行在控制台输出的结果
     */
    public static String execByRuntime(String cmd) {
        Process process = null;
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            inputStreamReader = new InputStreamReader(process.getInputStream(), "GBK");
            bufferedReader = new BufferedReader(inputStreamReader);

            int read;
            char[] buffer = new char[4096];
            StringBuilder output = new StringBuilder();
            while ((read = bufferedReader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != inputStreamReader) {
                try {
                    inputStreamReader.close();
                } catch (Throwable t) {

                }
            }
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (Throwable t) {

                }
            }
            if (null != process) {
                try {
                    process.destroy();
                } catch (Throwable t) {

                }
            }
        }
    }
}
