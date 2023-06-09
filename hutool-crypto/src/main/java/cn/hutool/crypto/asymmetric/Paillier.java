package cn.hutool.crypto.asymmetric;

import cn.hutool.core.util.HexUtil;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * 同态加密算法Paillier
 *
 * 加法同态，存在有效算法+，E(x+y)=E(x)+E(y)或者 x+y=D(E(x)+E(y))成立，并且不泄漏 x 和 y。
 * 乘法同态，存在有效算法*，E(x×y)=E(x)*E(y)或者 xy=D(E(x)*E(y))成立，并且不泄漏 x 和 y。
 *
 * 方案安全性可以归约到判定性合数剩余假设（Decisional Composite Residuosity Assumption, DCRA），即给定一个合数n和整数z，判定z是否在n^2下是否是n次剩余是困难的。
 * 这个假设经过了几十年的充分研究，到目前为止还没有多项式时间的算法可以攻破，所以Paillier加密方案的安全性被认为相当可靠。
 *
 * 字符串文本加解密相互配对,此时无法使用同态加法和同态乘法
 * 数值类型不可使用字符串加解密
 *
 * 公钥加密和同态加法/同态乘法运算
 * 私钥解密
 *
 * @author Revers.
 **/
public class Paillier {

	//公钥 n g
    //私钥 n lambda u
    private static int bitLength = 2048;
	private static int certainty = 256;

	/**
	 * 生成密钥算法。（默认）
	 * @return PaillierKeyPair 公钥私钥对
	 */
	public static final PaillierKeyPair generateKey() {
		return generateKey(bitLength,certainty);
	}

	/**
	 * 生成密钥算法
	 *
	 * @param bitLength 密钥位数
	 * @param certainty 此构造函数的执行时间与此参数的值成比例。
	 * @return PaillierKeyPair 公钥私钥对
	 */
    public static final PaillierKeyPair generateKey(int bitLength,int certainty) {
		BigInteger p =new BigInteger(bitLength / 2, certainty, new SecureRandom());
		BigInteger q =new BigInteger(bitLength / 2, certainty, new SecureRandom());
		BigInteger n = p.multiply(q);
		BigInteger nSquare = n.multiply(n);
		BigInteger lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
				.divide(p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)));
		BigInteger g = n.add(BigInteger.ONE);
		BigInteger u = g.modPow(lambda, nSquare).subtract(BigInteger.ONE).divide(n).modInverse(n);
		PaillierpublicKey publicKey = new PaillierpublicKey(n,g);
		PaillierPrivateKey privateKey = new PaillierPrivateKey(n, lambda,u);
		PaillierKeyPair keyPair = new PaillierKeyPair(publicKey,privateKey);
		return keyPair;
    }

	/**
	 * 字符串加密算法。输入一个明文和一个公钥
	 * 只能使用字符串解密算法 decryptString ，不可使用同态加法和同态乘法
	 *
	 * @param text 明文，字符串形式
	 * @param publicKey 公钥
	 *
	 * @return byte[]密文
	 */
	public static final byte[] encryptString(String text, PaillierpublicKey publicKey) {
		BigInteger r = new BigInteger(bitLength, new Random());
		BigInteger n = publicKey.getN();
		BigInteger nsquare = n.multiply(n);
		return publicKey.getG().modPow( new BigInteger(HexUtil.encodeHexStr(text),16), nsquare).multiply(r.modPow(n, nsquare)).mod(nsquare).toByteArray();
	}

	/**
	 * 字符串解密算法。输入一个密文和一个私钥
	 *
	 * @param ciphertext byte[]密文
	 * @param privateKey 私钥
	 * @return 解密的明文
	 */
	public static final String decryptString(byte[] ciphertext, PaillierPrivateKey privateKey) {
		BigInteger n = privateKey.getN();
		BigInteger lambda = privateKey.getLambda();
		BigInteger u = privateKey.getu();
		BigInteger nsquare = n.multiply(n);
		String s = new BigInteger(ciphertext).modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).multiply(u).mod(n).toString();
		return HexUtil.decodeHexStr(new BigInteger(s).toString(16));
	}

	/**
	 * 加密算法。输入一个明文和一个公钥
	 *
	 * @param text 明文，为防止字符串数据，只能使用 BigInteger类型
	 * @param publicKey 公钥
	 * @return byte[]密文
	 */
	public static final byte[] encrypt(BigInteger text, PaillierpublicKey publicKey) {
		BigInteger r = new BigInteger(bitLength, new Random());
		BigInteger n = publicKey.getN();
		BigInteger nsquare = n.multiply(n);
		return publicKey.getG().modPow(text, nsquare).multiply(r.modPow(n, nsquare)).mod(nsquare).toByteArray();
    }

	/**
	 * 解密算法。输入一个密文和一个私钥
	 *
	 * @param ciphertext byte[]密文
	 * @param privateKey 私钥
	 * @return 解密的明文
	 */
	public static final String decrypt(byte[] ciphertext, PaillierPrivateKey privateKey) {
		BigInteger n = privateKey.getN();
		BigInteger lambda = privateKey.getLambda();
		BigInteger u = privateKey.getu();
		BigInteger nsquare = n.multiply(n);
		return new BigInteger(ciphertext).modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).multiply(u).mod(n).toString();
    }

	/**
	 * 同态标量加算法。输入两个密文和一个公钥
	 *
	 * @param ciphertext 密文1，BigInteger
	 * @param ciphertext2 密文2，BigInteger
	 * @param publicKey 公钥
	 * @return byte[]密文
	 */
	public static final byte[] add(BigInteger ciphertext,BigInteger ciphertext2,PaillierpublicKey publicKey){
		return ciphertext.add(ciphertext2).multiply(publicKey.getN()).toByteArray();
	}

	/**
	 * 同态标量加算法。输入两个密文和一个公钥
	 *
	 * @param ciphertext 密文1，10进制字符串
	 * @param ciphertext2 密文2，10进制字符串
	 * @param publicKey 公钥
	 * @return byte[]密文
	 */
	public static final byte[] add(String ciphertext,String ciphertext2,PaillierpublicKey publicKey){
		return new BigInteger(ciphertext).multiply(new BigInteger(ciphertext2)).mod(publicKey.getN().multiply(publicKey.getN())).toByteArray();
	}

	/**
	 * 同态标量加算法。输入两个密文和一个公钥
	 *
	 * @param ciphertext byte[] 10进制字符串
	 * @param ciphertext2 byte[] 10进制字符串
	 * @param publicKey 公钥
	 * @return byte[]密文
	 */
	public static final byte[] add(byte[] ciphertext,byte[] ciphertext2,PaillierpublicKey publicKey){
		return new BigInteger(ciphertext).multiply(new BigInteger(ciphertext2)).mod(publicKey.getN().multiply(publicKey.getN())).toByteArray();
	}

	/**
	 * 同态标量乘算法。输入一个密文和一个标量明文和一个公钥
	 *
	 * @param ciphertext 密文，BigInteger
	 * @param number 明文，10进制字符串
	 * @param publicKey 公钥
	 * @return byte[]密文
	 */
	public static final byte[] multiply(BigInteger ciphertext,BigInteger number,PaillierpublicKey publicKey){
		return ciphertext.modPow(number,publicKey.getN().multiply(publicKey.getN())).toByteArray();
	}

	/**
	 * 同态标量乘算法。输入一个密文和一个标量明文和一个公钥
	 *
	 * @param ciphertext 密文，byte[]
	 * @param number 明文，10进制字符串
	 * @param publicKey 公钥
	 * @return byte[]密文
	 */
	public static final byte[] multiply(String ciphertext,BigInteger number,PaillierpublicKey publicKey){
		return new BigInteger(ciphertext).modPow(number,publicKey.getN().multiply(publicKey.getN())).toByteArray();
	}

	/**
	 * 同态标量乘算法。输入一个密文和一个标量明文和一个公钥
	 *
	 * @param ciphertext 密文，byte[]
	 * @param number 明文，10进制字符串
	 * @param publicKey 公钥
	 * @return byte[]密文
	 */
	public static final byte[] multiply(byte[] ciphertext,BigInteger number,PaillierpublicKey publicKey){
		return new BigInteger(ciphertext).modPow(number,publicKey.getN().multiply(publicKey.getN())).toByteArray();
	}
}