package com.cat.code.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: 
 * @Desc:
 * @Time: 10:05 2021/8/2
 **/
@Slf4j
public class CsvUtils {

  /**
   * CSV文件列分隔符
   */
  public static final String CSV_COLUMN_SEPARATOR = ",";

  /**
   * CSV文件行分隔符
   */
  public static final String CSV_ROW_SEPARATOR = System.lineSeparator();


  /**
   * 获取标题行
   *
   * @param headerArr 标题数组
   */
  public static String getTitleLine(String[] headerArr) {
    StringBuffer line = new StringBuffer("");
    for (String title : headerArr) {
      line.append(title).append(CSV_COLUMN_SEPARATOR); //添加标题行数据
    }
    line.append(CSV_ROW_SEPARATOR); //换行数据
    return line.toString();
  }

  /**
   * 获取数据行
   *
   * @param fieldArr 字段数组
   * @param obj      实体对象
   */
  public static String getRowLine(String[] fieldArr, Object obj) {
    return getRowLine(fieldArr, null, obj);
  }

  /**
   * 获取数据行
   *
   * @param fieldArr     字段数组
   * @param fieldTypeArr 字段类型数组
   * @param obj          实体对象
   */
  public static String getRowLine(String[] fieldArr, Integer[] fieldTypeArr, Object obj) {
    StringBuffer lineBf = new StringBuffer("");

    Set<String> objFieldSet = getObjectFields(obj);
    for (int i = 0; i < fieldArr.length; i++) {
      String fieldName = fieldArr[i];
      int fieldType = fieldTypeArr != null && fieldTypeArr.length > i && fieldTypeArr[i] != null
          ? fieldTypeArr[i] : 0;
      try {
        if (fieldName.contains(".")) {
          lineBf.append(getMultiFieldValue(obj, objFieldSet, fieldName, fieldType));
        } else if (objFieldSet.contains(fieldName)) {
          lineBf.append(getFieldValue(obj, fieldName, fieldType));
        } else {
          lineBf.append(fieldType == 1 ? 0 : "");
        }
      } catch (Exception ex) {
        log.error("get the csv column data failed:", ex);
      } finally {
        lineBf.append(CSV_COLUMN_SEPARATOR); //CSV间隔数据
      }
    }
    lineBf.append(CSV_ROW_SEPARATOR);//换行数据
    return lineBf.toString();
  }

  //获取复杂对象变量
  private static Object getMultiFieldValue(Object obj, Set<String> objFieldSet, String fieldName,
      int fieldType) throws Exception {

    Object value = obj;
    Set<String> valueFieldSet = objFieldSet;

    String[] fieldNameArr = fieldName.split("\\.");
    for (String name : fieldNameArr) {
      if (valueFieldSet.contains(name)) {
        value = getFieldValue(value, name);
        if (value != null) {
          valueFieldSet = getObjectFields(value);
        }
      } else {
        value = null;
      }
    }
    return convertValue(value, fieldType);
  }

  private static Object getFieldValue(Object obj, String fieldName)
      throws Exception {
    if (obj == null) {
      return null;
    }
    Class<?> srcClass = obj.getClass();
    Field objField = srcClass.getDeclaredField(fieldName);
    if (objField == null) {
      return null;
    }
    objField.setAccessible(true); //设置private可访问
    return objField.get(obj);
  }

  private static Object getFieldValue(Object obj, String fieldName, int fieldType)
      throws Exception {
    Object value = getFieldValue(obj, fieldName);
    return convertValue(value, fieldType);
  }

  //转化数值格式
  private static String convertValue(Object value, int fieldType) {
    Object newValue = value;
    if (value == null) {
      if (fieldType == 1 || fieldType == 2) {
        newValue = 0;
      } else {
        newValue = "";
      }
    }

    if (fieldType == 2) {
      try {
        double dValue = Double.parseDouble(newValue.toString().trim()) * 100;
        return String.format("%.2f", dValue) + "%";
      } catch (Exception ex) {
        log.error("Convert data to double failed, value:{}", newValue, ex);
        //替换英文 , 为中文 ， csv格式由,分割
        return newValue.toString().replaceAll(",", "，");
      }
    } else {
      return newValue.toString().replaceAll(",", "，");
    }
  }

  private static Set<String> getObjectFields(Object obj) {
    //获取Obj 所有字段
    Set<String> objFieldSet = new HashSet<>();
    Field[] fields = obj.getClass().getDeclaredFields();
    for (Field field : fields) {
      objFieldSet.add(field.getName());
    }
    return objFieldSet;
  }

  // 获取二级复合型 对象的字段
  private static Map<String, String> getComplexObjectFields(Object obj) {
    //获取Obj 所有字段
    Map<String, String> objFieldMap = new HashMap<>();
    Field[] fields = obj.getClass().getDeclaredFields();
    for (Field field : fields) {
      try {
        //获取二级
        field.setAccessible(true); //设置private可访问
        Object sonObj = field.get(obj);
        if (sonObj == null) {
          continue;
        }
        Field[] sonFields = sonObj.getClass().getDeclaredFields();
        //变量二级
        for (Field sonField : sonFields) {
          // 在找二级时查出父级
          objFieldMap.put(sonField.getName(), field.getName());
        }
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }
    return objFieldMap;
  }

  /**
   * 获取数据行
   *
   * @param fieldArr     字段数组
   * @param fieldTypeArr 字段类型数组
   * @param obj          实体对象
   */
  public static String getComplexRowLine(String[] fieldArr, Integer[] fieldTypeArr, Object obj) {
    StringBuffer lineBf = new StringBuffer("");

    Map<String, String> objFieldMap = getComplexObjectFields(obj);
    for (int i = 0; i < fieldArr.length; i++) {
      String fieldName = fieldArr[i];
      int fieldType = fieldTypeArr != null && fieldTypeArr.length > i && fieldTypeArr[i] != null
          ? fieldTypeArr[i] : 0;
      try {
        if (objFieldMap.containsKey(fieldName)) {
          //获取父级 对象
          Object parent = getFieldValue(obj, objFieldMap.get(fieldName));
          //根据父级对象获取具体的变量值
          lineBf.append(getFieldValue(parent, fieldName, fieldType));
        } else {
          lineBf.append(fieldType == 1 ? 0 : "");
        }
      } catch (Exception ex) {
        log.error("get the csv column data failed:", ex);
      } finally {
        lineBf.append(CSV_COLUMN_SEPARATOR); //CSV间隔数据
      }
    }
    lineBf.append(CSV_ROW_SEPARATOR);//换行数据
    return lineBf.toString();
  }
}
