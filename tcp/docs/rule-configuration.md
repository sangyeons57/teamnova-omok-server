# Rule Selection Configuration

## 운영 기본값
- 서버는 `RuleManager`가 `RuleRegistry`에 등록된 규칙 중에서 플레이어 최저 점수에 맞는 후보를 무작위로 선택합니다.
- 기본 등록 규칙은 다음과 같습니다: `FIVE_TURN_RANDOM_BLOCKER`, `PER_TURN_ADJACENT_BLOCKER`, `EVERY_TWO_TURN_JOKER`, `GO_CAPTURE`, `ROTATE_STONES`.

## 고정 규칙 오버라이드
- 개발/테스트 중 특정 규칙만 강제로 적용하려면 애플리케이션 시작 전에 인메모리 설정을 변경합니다.
- `RuleSelectionConfig.configureFixedRules(...)`에 규칙 ID 리스트를 전달하면 **랜덤 선택이 완전히 무시**되고, 지정한 규칙 전부가 매 턴 종료 시 실행됩니다.

### 설정 방법 예시
```java
// e.g. Main.main 실행 초기에 추가
RuleSelectionConfig.configureFixedRules(
    RuleId.GO_CAPTURE,
    RuleId.ROTATE_STONES
);
```

### 참고 사항
- `RuleId` 열거형 값을 그대로 사용합니다. `null`이나 중복은 무시됩니다.
- 등록되지 않은 규칙을 전달하면 레지스트리 조회 시 건너뛰며 로그가 출력됩니다.
- 오버라이드가 활성화되면 세션의 `desiredRuleCount`는 지정된 규칙 수로 설정됩니다.
- 임시 적용 후 기본 랜덤 동작으로 되돌리고 싶다면 `RuleSelectionConfig.clearFixedRules()`를 호출합니다.

## 로그
- `[RULE_LOG] Using fixed rule override ...` : 고정 규칙이 적용되었음을 의미합니다.
- `[RULE_LOG] Fixed rule ... not registered` : 지정한 규칙이 아직 레지스트리에 등록되지 않았습니다.
- `[RULE_LOG] Fixed rule override configured but no rules resolved` : 모든 고정 규칙이 무시되어 랜덤 선택으로 복귀했습니다.
