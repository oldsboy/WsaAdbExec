import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

public class AdbExec {
    public static void main(String[] args) {
        String devices = execByRuntime("cmd /c adb devices -l");
        if (devices == null || !devices.startsWith("List of devices attached")) {
            System.out.println("估计是adb没安装或者adb的系统环境变量没有配置");
            return;
        }

        select_device_flow(args);
    }

    private static void select_device_flow(String[] args) {
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

        String dn = null;
        if (device_list.size() == 1) {
            dn = device_list.get(0);
            System.out.println("当前仅有一个连接,自动使用连接->"+dn);
        }else {
            dn = select_device(device_list);
        }

        main_menu(dn, args);
    }

    /**
     * @param device_name 设备名
     * @param args
     * @description 主菜单
     * @author oldsboy; @date 2022-10-09 16:47
     */
    private static void main_menu(String device_name, String[] args) {
        if (args != null && args.length > 0) {
            System.out.println("正在自动安装...");
            installApp(device_name, args[0]);
            System.out.println("安装完成,输入任何退出程序");
            new Scanner(System.in).next();
            System.exit(0);
            return;
        }

        System.out.printf("当前设备:%s-输入编号以执行命令:\n", device_name);
        System.out.println("0.重新选择设备");
        System.out.println("1.查看所有第三方app包名");
        System.out.println("2.查看设备信息");
        System.out.println("3.安装应用");
        System.out.println("4.卸载应用");
        System.out.println("c.执行自定义adb语句");
        System.out.println("q.退出");

        switch (new Scanner(System.in).next()) {
            case "0":
                select_device_flow(null);
                return;
            case "1":
                System.out.println(execByRuntime("adb -s " + device_name + " shell pm list packages -3"));
                break;
            case "2":
                showDeviceInfo(device_name);
                break;
            case "3":
                System.out.println("将apk拖到这里来,或者输入apk路径:");
                String path = new Scanner(System.in).nextLine();
                installApp(device_name, path);
                break;
            case "4":
                uninstallApplication(device_name);
                break;
            case "c":
                System.out.println("请输入cmd语句:");
                System.out.println(execByRuntime(new Scanner(System.in).nextLine()));
                break;
            case "q":
                System.exit(0);
                return;
        }

        main_menu(device_name, null);
    }

    private static boolean installApp(String device_name, String path) {
        File file = new File(path);
        if (!file.exists() || !file.getName().contains("apk")) {
            System.out.println("文件格式不标准");
            return false;
        }
        System.out.println(execByRuntime("adb -s " + device_name + " install -r "+path));
        return true;
    }

    private static void uninstallApplication(String device_name) {
        System.out.println("当前的操作是卸载应用,请谨慎操作");

        String packages = execByRuntime("adb -s " + device_name + " shell pm list packages -3");
        int index = 0;
        ArrayList<String> package_info_list = new ArrayList<>();
        ArrayList<String> package_list = new ArrayList<>();
        for (String line : packages.split("\\r\\n")) {
            package_info_list.add("编号-"+index+": 包名:"+line.replace("package:", ""));
            package_list.add(line.replace("package:", ""));
            index++;
        }

        for (String pkg : package_info_list) {
            System.out.println(pkg);
        }

        System.out.println("输入编号以卸载应用:");
        int code = new Scanner(System.in).nextInt();

        System.out.printf("你要删除的应用的包是:%s,是否确认要卸载?(y/n)", package_list.get(code));
        String b = new Scanner(System.in).next();
        if (b.equals("y")) {
            System.out.println(execByRuntime("adb -s " + device_name + " uninstall "+package_list.get(code)));
        }
        System.out.println();
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
