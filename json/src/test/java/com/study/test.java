package com.study;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

/**
 * @author yangz
 * @date 2022/4/13 - 16:04
 */
public class test {
    @Test
    public void t1(){
     String s="{\"topic_name\":\"上学期,勇于探究,\",\"classify\":\"602517889430102016,602521520686538752,619206933895520256,\"}";
        JSONObject jsonObject = (JSONObject) JSON.parse(s);
        String topic_name = (String)jsonObject.get("classify");
        System.out.println(topic_name);

    }
}
