package com.example.test;


import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class JavaForExcel {

    public static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

    public static void createExcel(Context context) {
        try {
            //创建文件
            WritableWorkbook book = null;
            String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File(absolutePath, getFileName());
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    Toast.makeText(context, "文件创建成功", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(context, "文件创建失败", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                Log.d("JavaForExcel", "file.getAbsolutePath():" + file.getAbsolutePath());
                book = Workbook.createWorkbook(file);
                WritableSheet sheet = book.createSheet("Sheet1", 0);
                Label label = new Label(0, 0, "时间");
                Label labe2 = new Label(1, 0, "糖度值");
                Label labe3 = new Label(2, 0, "经度");
                Label labe4 = new Label(3, 0, "纬度");
                sheet.addCell(label);
                sheet.addCell(labe2);
                sheet.addCell(labe3);
                sheet.addCell(labe4);
                book.write();
                book.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean writeToExcel( String date, String message, double longitude, double latitude) {
        try {
            String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            File excelFile = new File(absolutePath, getFileName());
            if (excelFile.exists()) {
                Workbook oldWwb = Workbook.getWorkbook(excelFile);
                WritableWorkbook wwb = Workbook.createWorkbook(excelFile, oldWwb);
                //获取指定索引的表格
                WritableSheet ws = wwb.getSheet(0);
                // 获取该表格现有的行数
                int row = ws.getRows();
                Label lbl1 = new Label(0, row, date);
                ws.addCell(lbl1);
                Label bll2 = new Label(1, row, message);
                ws.addCell(bll2);
                Label bll3 = new Label(2, row, String.valueOf(longitude));
                ws.addCell(bll3);
                Label bll4 = new Label(3, row, String.valueOf(latitude));
                ws.addCell(bll4);
                // 从内存中写入文件中,只能刷一次.
                wwb.write();
                wwb.close();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getFileName() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = new Date(System.currentTimeMillis());
        // filename:Data_2021-09-13.xlsx
        return "Data_" + format.format(date) + ".xlsx";
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 将 GCJ-02 坐标转换成 BD-09 坐标
     *
     * @param lat
     * @param lon
     */
    public static double[] gcj02_To_Bd09(double lat, double lon) {
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
        double tempLon = z * Math.cos(theta) + 0.0065;
        double tempLat = z * Math.sin(theta) + 0.006;
        double[] gps = {tempLat, tempLon};
        return gps;
    }
}
