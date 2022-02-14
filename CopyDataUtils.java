package com.chujian.fms.marketing.utils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: lvgang
 * @Desc:
 * @Time: 11:48 2021/7/22
 **/
@Slf4j
public class CopyDataUtils {

  /**
   * 将srcObj 的数据拷贝到 tarObject 相同字段上
   *
   * @param srcObj
   * @param tarObj
   */
  public static Object copyFieldData(Object srcObj, Object tarObj) {
    try {
      //获取源数据类对象
      Class<?> srcClass = srcObj.getClass();
      //制造源数据对象集合
      Field[] srcFields = srcClass.getDeclaredFields();
      Set<String> srcSet = new HashSet<>();
      for (Field srcField : srcFields) {
        srcSet.add(srcField.getName());
      }
      //获取目标对象 字段field
      Field[] tarFields = tarObj.getClass().getDeclaredFields();
      //遍历目标对象
      for (Field tarField : tarFields) {
        //确保该字段值是存在的，避免NoSuchField 错误
        if (srcSet.contains(tarField.getName())) {
          //在源数据对象中找到对应的值
          Field srcField = srcClass.getDeclaredField(tarField.getName());

          srcField.setAccessible(true); //设置private可访问
          Object value = srcField.get(srcObj);
          if (value != null) {
            //赋值到目标对象上
            tarField.setAccessible(true);
            tarField.set(tarObj, value);
          }
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return tarObj;
  }
}
