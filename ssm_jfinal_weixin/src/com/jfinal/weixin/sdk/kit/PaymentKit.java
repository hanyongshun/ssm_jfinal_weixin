package com.jfinal.weixin.sdk.kit;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jfinal.kit.HashKit;
import com.jfinal.kit.StrKit;
import com.jfinal.weixin.sdk.utils.Charsets;
import com.jfinal.weixin.sdk.utils.HttpUtils;

/**
 * 微信支付的统一下单工具类
 * @author L.cm
 */
public class PaymentKit {
	
	/**
	 * 组装签名的字段
	 * @param params 参数
	 * @param urlEncoder 是否urlEncoder
	 * @return String
	 */
	public static String packageSign(Map<String, String> params, boolean urlEncoder) {
		// 先将参数以其参数名的字典序升序进行排序
		TreeMap<String, String> sortedParams = new TreeMap<String, String>(params);
		// 遍历排序后的字典，将所有参数按"key=value"格式拼接在一起
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Entry<String, String> param : sortedParams.entrySet()) {
			String value = param.getValue();
			if (StrKit.isBlank(value)) {
				continue;
			}
			if (first) {
				first = false;
			} else {
				sb.append("&");
			}
			sb.append(param.getKey()).append("=");
			if (urlEncoder) {
				try { value = urlEncode(value); } catch (UnsupportedEncodingException e) {}
			}
			sb.append(value);
		}
		return sb.toString();
	}
	
	/**
	 * urlEncode
	 * @param src 微信参数
	 * @return String
	 * @throws UnsupportedEncodingException
	 */
	public static String urlEncode(String src) throws UnsupportedEncodingException {
		return URLEncoder.encode(src, Charsets.UTF_8.name()).replace("+", "%20");
	}
	
	/**
	 * 生成签名
	 * @return
	 */
	public static String createSign(Map<String, String> params, String paternerKey) {
		// 生成签名前先去除sign
		params.remove("sign");
		String stringA = packageSign(params, false);
		String stringSignTemp = stringA + "&key=" + paternerKey;
		return HashKit.md5(stringSignTemp).toUpperCase();
	}
	
	/**
	 * 支付异步通知时校验sign
	 * @param params
	 * @param paternerKey
	 * @return
	 */
	public static boolean verifyNotify(Map<String, String> params, String paternerKey){
		String sign = params.get("sign");
		String localSign = PaymentKit.createSign(params, paternerKey);
		return sign.equals(localSign);
	}
	
	/**
	 * 微信下单，map to xml
	 * @param params 参数
	 * @return String
	 */
	public static String toXml(Map<String, String> params) {
		StringBuilder xml = new StringBuilder();
		xml.append("<xml>");
		for (Entry<String, String> entry : params.entrySet()) {
			String key   = entry.getKey();
			String value = entry.getValue();
			// 略过空值
			if (StrKit.isBlank(value)) continue;
			xml.append("<").append(key).append(">");
				xml.append(entry.getValue());
			xml.append("</").append(key).append(">");
		}
		xml.append("</xml>");
		return xml.toString();
	}
	
	/**
	 * 威富通下单，map to xml
	 * @param params 参数
	 * @return String
	 */
	public static String wtoXml(Map<String, String> params) {
		StringBuilder xml = new StringBuilder();
		xml.append("<xml>");
		for (Entry<String, String> entry : params.entrySet()) {
			String key   = entry.getKey();
			String value = entry.getValue();
			// 略过空值
			if (StrKit.isBlank(value)) continue;
			xml.append("<").append(key).append(">");
				xml.append("<![CDATA["+entry.getValue()+"]]>");
			xml.append("</").append(key).append(">");
		}
		xml.append("</xml>");
		return xml.toString();
	}
	
	/**
	 * 针对支付的xml，没有嵌套节点的简单处理
	 * @param xml xml字符串
	 * @return Map<String, String> map集合
	 */
	public static Map<String, String> xmlToMap(String xmlStr) {
		Document document = XmlKit.parse(xmlStr);
		Element root = document.getDocumentElement();
		Map<String, String> params = new HashMap<String, String>();
		
		// 将节点封装成map形式
		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			params.put(node.getNodeName(), node.getTextContent());
		}
		// 含有空白符会生成一个#text参数
		params.remove("#text");
		return params;
	}
	
	/**
	 * 申请退款，内部添加了随机字符串nonce_str和签名sign
	 * @param params 参数map，内部添加了随机字符串nonce_str和签名sign
	 * @param paternerKey 商户密钥
	 * @param certPath 证书文件目录
	 * @return Map<String, String> map
	 * @throws Exception 
	 */
	public static String refundUrl = "https://api.mch.weixin.qq.com/secapi/pay/refund";
	public static Map<String, String> refund(Map<String, String> params, String paternerKey, String certPath) {
		params.put("nonce_str", System.currentTimeMillis() + "");
		String sign = PaymentKit.createSign(params, paternerKey);
		params.put("sign", sign);
		String partner = params.get("mch_id");
		String xmlStr = HttpUtils.postSSL(refundUrl, PaymentKit.toXml(params), certPath, partner);
		System.out.println(xmlStr);
		return PaymentKit.xmlToMap(xmlStr);
	}

}