package com.bknote71.codecraft;

import com.bknote71.codecraft.robocode.core.battle.Battle;
import com.bknote71.codecraft.robocode.core.battle.BattleManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodeCraftApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeCraftApplication.class, args);

		// 1 번 배틀 시작
		Battle battle = BattleManager.Instance.startNewBattle();
	}
}
