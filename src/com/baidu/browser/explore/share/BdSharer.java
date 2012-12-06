/** 
 * Filename:    Share.java 
 * Description:  
 * Copyright:   Baidu MIC Copyright(c)2011 
 * @author:     gong.haiping 
 * @version:    1.0 
 * Create at:   2011-7-1 ����05:17:54 
 * 
 * Modification History: 
 * Date         Author      Version     Description 
 * ------------------------------------------------------------------ 
 * 2011-7-1   gong.haiping      1.0         1.0 Version 
 */
package com.baidu.browser.explore.share;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.view.KeyEvent;

import com.baidu.browser.core.util.BdLog;
import com.baidu.browser.explore.share.BdTinyUrlGenerator.BdTinyUrlRecievedListener;
import com.baidu.player.R;

/**
 * ���ݷ�����
 * <p>
 * ֧���ı���ͼƬ����
 * </p>
 * 
 */
public class BdSharer { // SUPPRESS CHECKSTYLE

	/**
	 * Ĭ�Ϸ���������
	 */
	public static final int SHARE_RERQUEST_CODE_DEFAULT = 1996;
	/**
	 * ��ͼ����������
	 */
	public static final int SHARE_RERQUEST_CODE_SCREENSHOT = 1997;
	/**
	 * ���ط���������
	 */
	public static final int SHARE_RERQUEST_CODE_DOWNLOAD = 1998;
	/**
	 * ҳ���ڷ���������
	 */
	public static final int SHARE_RERQUEST_CODE_WEB = 1999;

	/**
	 * �ı����������������
	 */
	private static final int SHARE_TEXT_SIZE_LIMIT = 140;
	/**
	 * �����ȴ���ʾ��
	 */
	private static BdWaitingDialog waitProgressDialog;

	/**
	 * ���ڶ���ַ�ĵȴ���ȡ��
	 */
	private static BdWaitingDialog waitTinyUrlDialog;
	/**
	 * �Ƿ�ȡ������
	 */
	private static boolean willCancelShare = false;

	/**
	 * ͼƬ������С��ֵ��WebkitĬ������ѡ��ͼƬ���� ���ͬʱ��ͼƬ�����֣�������ͼƬ�����ֻ��Ĭ�����ֵ�ͼƬ����ͼƬ��СС�ڴ˷�ֵ�ģ���Ϊ���ַ���
	 */
	public static final long IMAGE_MIN_SIZE_TO_SHARE = 1024 * 10;

	/**
	 * ȡ��������ֹ���߳�ȡ���������������̲߳�����ȡȡ��״̬���󣬵�����Ȼ�򿪵���������������
	 */
	private static Object cancelLock = new Object();

	/**
	 * �Ƿ��ͳɹ�
	 */
	private static boolean sendOk;

	/**
	 * �����ı�
	 * 
	 * @param mContext
	 *            app������
	 * @param text1
	 *            �ı������ֶ�һ
	 * @param text2
	 *            �ı������ֶζ�
	 * @return �Ƿ��ͳɹ�
	 */
	public static boolean sendTextShare(Context mContext, String text1, String text2) {
		return sendTextShare(mContext, R.string.share_content, text1, text2);
	}

	/**
	 * �����ı�
	 * 
	 * @param mContext
	 *            app������
	 * @param templateId
	 *            �ı�����ģ��
	 * @param text1
	 *            �ı������ֶ�һ
	 * @param text2
	 *            �ı������ֶζ�
	 * @return �Ƿ��ͳɹ�
	 */
	public static boolean sendTextShare(final Context mContext, final int templateId, final String text1,
			final String text2) {

		BdLog.d(text1 + ", " + text2);

		final BdSharerOM shareContentMeta = new BdSharerOM();
		shareContentMeta.setContentType(BdSharerOM.TYPE_TEXT);

		waitTinyUrlDialog = new BdWaitingDialog(mContext);
		waitTinyUrlDialog.setMessage(mContext.getString(R.string.share_waiting));
		waitTinyUrlDialog.setCancelable(true);
		waitTinyUrlDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				willCancelShare = true;
			}

		});

		waitTinyUrlDialog.show();

		willCancelShare = false;
		BdTinyUrlGenerator generator = new BdTinyUrlGenerator();
		generator.setEventListener(new BdTinyUrlRecievedListener() {

			@Override
			public void onTinyUrlRecieved(String tinyUrl) {
				String extra1 = text1;
				String extra2 = tinyUrl;
				if (text1 == null) {
					extra1 = "";
				}
				//				if (extra1 != "") {
				//					extra1 = text1 + TextUtil.COMMA;
				//				}
				if (tinyUrl == null) {
					extra2 = "";
				}
				//				if (extra2 != "") {
				//					extra2 = Sharer.SHARE_DETAIL_TIP + tinyUrl;
				//				}
				if (!"".equals(extra1) && "".equals(extra2)) {
					extra1 = text1;
				}
				BdLog.d(extra1 + ", " + extra2);
				String text = mContext.getString(templateId, extra1, extra2);
				shareContentMeta.setContent(text);
				shareContentMeta.setExtra1(text1);
				shareContentMeta.setExtra2(tinyUrl);
				shareContentMeta.setContent(preProcContent(mContext, shareContentMeta));
				if (!willCancelShare) {
					sendOk = BdSharer.sendShare(mContext, shareContentMeta, false,
							SHARE_RERQUEST_CODE_DEFAULT, false);
				}
				if (waitTinyUrlDialog != null) {
					waitTinyUrlDialog.cancel();
					waitTinyUrlDialog = null;
				}
			}
		});

		generator.generate(text2);

		return sendOk;
	}

	/**
	 * ���ͷ���
	 * 
	 * @param mContext
	 *            app������
	 * @param shareContentMeta
	 *            ��������Ԫ����
	 * @param isMultiple
	 *            �Ƿ�ͬʱ���Ͷ�������
	 * @param shareEntry
	 *            ������ڣ�Ŀǰ����ҳ�ı�����ҳͼƬ����Ļ��ͼ����ǩ����ʷ�����ع����������
	 * @param isCheckPriority
	 *            �Ƿ���Ҫ���������ȼ�
	 * @return �Ƿ�����ɹ�
	 */
	public static boolean sendShare(Context mContext, BdSharerOM shareContentMeta, boolean isMultiple,
			int shareEntry, boolean isCheckPriority) {

		if (shareContentMeta == null || shareContentMeta.getContent() == null) {
			BdLog.e("shareContentMeta is null.");
			return false;
		}

		String action = "android.intent.action.SEND";
		Intent intent = new Intent(action);
		// ������û�и������������ı���Ϣ������������ͼƬ�������ܻ᲻֧��android.intent.extra.TEXT
		// ��������֣����������
		intent.putExtra("android.intent.extra.TEXT", shareContentMeta.getContent());
		BdLog.d(shareContentMeta.getContent());
		int contentType = shareContentMeta.getContentType();
		if (contentType == BdSharerOM.TYPE_TEXT) {
			intent.setType("text/plain");
		}

		Intent intent2 = Intent.createChooser(intent, mContext.getString(R.string.share_channel));
		try {
			if (mContext instanceof Activity) {
				Activity act = (Activity) mContext;
				act.startActivityForResult(intent2, shareEntry);
			}
		} catch (ActivityNotFoundException e) {
			BdLog.e("share app not found.", e);
			return false;
		} catch (Exception e) {
			BdLog.e("share exception.", e);
			return false;
		}
		return true;
	}

	/**
	 * Ԥ����Ҫ�������ı����ݣ�����
	 * <p>
	 * ���������ݺ�΢��������׼������140�֣�������Ӣ�ģ��������ţ�ʱ��[�ֶ�һ]���������ԡ�������ʾʣ�ಿ�֣�
	 * ������[�ֶ�һ]ȫ���ԡ�������ʾȴ��Ȼ��������ʱ��ɾ�����������[�ֶζ�]������������ʾ[�ֶ�һ]�е����ݣ�
	 * </p>
	 * 
	 * @param mContext
	 *            Context
	 * @param shareContentMeta
	 *            ����Ԫ����
	 * @return ���������ı�����
	 */
	private static String preProcContent(Context mContext, BdSharerOM shareContentMeta) {

		String newContent = shareContentMeta.getContent();
		if (newContent.length() > SHARE_TEXT_SIZE_LIMIT) {
			// [�ֶ�һ]���������ԡ�������ʾʣ�ಿ��
			newContent = mContext.getString(R.string.share_content, "...", shareContentMeta.getExtra2());
			if (newContent.length() > SHARE_TEXT_SIZE_LIMIT) {
				// ɾ�����������[�ֶζ�]��
				newContent = mContext.getString(R.string.share_content, shareContentMeta.getExtra1(), "");
				if (newContent.length() > SHARE_TEXT_SIZE_LIMIT) {
					// [�ֶ�һ]���������ԡ�ǰ137����+������ʾʣ�ಿ�֣�����ɾ�����������[�ֶζ�]��
					String ellipsis = "...";
					newContent = newContent.substring(0, SHARE_TEXT_SIZE_LIMIT - ellipsis.length())
							+ ellipsis;
				}
			}
		}

		return newContent;
	}

	/**
	 * ���������ȴ��Ի���
	 * 
	 * @param mContext Context
	 */
	public static void showShareWaitDialog(Context mContext) {

		if (waitProgressDialog == null) {
			// waitProgressDialog = ProgressDialog.show(mContext, null, mContext
			// .getString(R.string.share_waiting), false, true);
			waitProgressDialog = new BdWaitingDialog(mContext);
			waitProgressDialog.setMessage(mContext.getString(R.string.share_waiting));
			waitProgressDialog.setOnKeyListener(new OnKeyListener() {

				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						return cancelShare();
					}
					return false;
				}

			});

		}
		waitProgressDialog.show();
	}

	/**
	 * �û�������ȡ������
	 * 
	 * @return �Ƿ�ɹ�ȡ���ȴ���ʾ��
	 */
	public static boolean cancelShare() {
		if (waitProgressDialog.isShowing()) { // ȡ������
			waitProgressDialog.dismiss();
			waitProgressDialog = null;
			return true;
		}
		return false;
	}

	/**
	 * @return the waitProgressDialog
	 */
	public static BdWaitingDialog getWaitProgressDialog() {
		return waitProgressDialog;
	}

	/**
	 * @return the waitTinyUrlDialog
	 */
	public static BdWaitingDialog getWaitTinyUrlDialog() {
		return waitTinyUrlDialog;
	}

	/**
	 * ȡ������
	 * 
	 * @return �Ƿ�ɹ�ȡ���ȴ���ʾ��
	 */
	public static boolean dismissWatiTinyUrlDialog() {
		if (waitTinyUrlDialog != null) {
			waitTinyUrlDialog.cancel();
			waitTinyUrlDialog.dismiss();
			waitTinyUrlDialog = null;
			return true;
		}
		return false;
	}

	/**
	 * @param waitProgressDialog the waitProgressDialog to set
	 */
	public static void setWaitProgressDialog(BdWaitingDialog waitProgressDialog) { // SUPPRESS CHECKSTYLE
		BdSharer.waitProgressDialog = waitProgressDialog;
	}

	/**
	 * @return the cancelLock
	 */
	public static Object getCancelLock() {
		return cancelLock;
	}

}