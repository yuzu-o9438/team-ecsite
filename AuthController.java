package jp.co.internous.panama.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import jp.co.internous.panama.model.domain.MstUser;
import jp.co.internous.panama.model.form.UserForm;
import jp.co.internous.panama.model.mapper.MstUserMapper;
import jp.co.internous.panama.model.mapper.TblCartMapper;
import jp.co.internous.panama.model.session.LoginSession;


/**
 * 認証に関する処理を行うコントローラー
 * @author yuzu-o9438
 *
 */
@RestController
@RequestMapping("/panama/auth")
public class AuthController {
	
	/*
	 * フィールド定義
	 */
	
	@Autowired
	private MstUserMapper userMapper;
	
	@Autowired
	private TblCartMapper cartMapper;
	
	@Autowired
	private LoginSession loginSession;
	
	
	private Gson gson = new Gson();
		
	/**
	 * ログイン処理をおこなう
	 * @param f ユーザーフォーム
	 * @return ログインしたユーザー情報(JSON形式)
	 */
	@PostMapping("/login")
	public String login(@RequestBody UserForm f) {
		
		//認証(DBの会員情報マスタテーブルにユーザー名(メールアドレス)とパスワードが一致するユーザーが存在しているかを確認)する。
		MstUser user = userMapper.findByUserNameAndPassword(f.getUserName(), f.getPassword());
		
		//ユーザーが存在した場合
		if (user != null) {
			
			// 仮ユーザーIDを取得
			int tmpUserId = loginSession.getTmpUserId(); 
			
			// カート情報のユーザーIDを更新
			cartMapper.updateUserId(user.getId(), tmpUserId);
			
			//ログイン情報をSessionに保存
			loginSession.setUserId(user.getId());
			loginSession.setTmpUserId(0);
			loginSession.setUserName(user.getUserName());
			loginSession.setPassword(user.getPassword());
			loginSession.setLogined(true);
			
		//ユーザが存在しない場合
		} else {
			loginSession.setUserId(0);
			loginSession.setUserName(null);
			loginSession.setPassword(null);
			loginSession.setLogined(false);
		}
		
		return gson.toJson(user);
	}
	
	/**
	 * ログアウト処理をおこなう
	 * @return 空文字
	 */
	@PostMapping("/logout")
	public String logout() {
		
		//Sessionの情報をnullに変更する。
		loginSession.setUserId(0);
		loginSession.setTmpUserId(0);
		loginSession.setUserName(null);
		loginSession.setPassword(null);
		loginSession.setLogined(false);
			
		return "";
	}

	/**
	 * パスワード再設定をおこなう
	 * @param f ユーザーフォーム
	 * @return 処理後のメッセージ
	 */
	@PostMapping("/resetPassword")
	public String resetPassword(@RequestBody UserForm f) {
		
		String newPassword = f.getNewPassword();
		
		//更新失敗した場合
		if(newPassword.isEmpty()) {
			return "未入力です。";
			
		} else if(newPassword.length() < 6) {
			return "文字数が最小値未満です。";
			
		} else if(newPassword.length() > 16) {
			return "文字数が最大値を超えています。";
			
		} else if(!newPassword.matches("[a-zA-Z0-9]+")){
			return "半角英数字以外の文字が含まれています。";
			
		}
		
		//更新成功した場合
		String userName = loginSession.getUserName();
			
		//パスワードをnewPasswordに更新
		userMapper.updatePassword(userName, newPassword);
			
		return "パスワードが再設定されました。";
	}
}
