# Turn 시스템 리팩토링 메모

## 1. 이번 변경 요약
- `TurnStore`가 `TurnOrder`·`TurnCounters`·`TurnTiming` VO를 품도록 개편해 턴 순서/라운드/시간 정보를 명확히 표현했습니다. (`src/main/java/teamnova/omok/glue/game/session/model/TurnStore.java`)
- 턴 스냅샷(`GameTurnService.TurnSnapshot`)이 확장된 메타데이터를 노출하고, 메시지 인코더와 상태 머신이 이 정보를 활용하도록 정리했습니다.
- 룰(`Every*Rule`)과 결과 계산(`OutcomeEvaluatingState`)이 기존 `turnNumber` 대신 `actionNumber`·`round`를 사용해 다중 사용자 턴 규칙을 구현할 수 있게 됐습니다.
- `TurnService`가 `TurnAdvanceStrategy`와 `SequentialTurnAdvanceStrategy`를 통해 다음 턴 선택 책임을 전략 객체에 위임하도록 리팩토링했습니다.
- 클라이언트 통신 메시지(`ReadyState`, `StonePlaced`, `TurnTimeout` 등)가 새 턴 구조를 직렬화하며, 상태 컨텍스트는 라운드/포지션을 보존합니다.

## 2. SOLID 관점 개선
- **단일 책임 원칙**: `SequentialTurnAdvanceStrategy`로 턴 선택 로직을 분리해 `TurnService`는 저장소 변이와 타이밍 계산에 집중합니다.
- **개방-폐쇄 원칙**: `TurnAdvanceStrategy` 인터페이스로 향후 규칙 기반/무작위 턴 전략을 추가할 수 있도록 확장 지점을 마련했습니다.
- **의존 역전 원칙**: `TurnService`가 구체 전략 대신 인터페이스에 의존하도록 변경해 고수준 모듈이 저수준 구현 세부사항에 묶이지 않습니다.
- **인터페이스 분리 원칙**: 턴 전략이 독립 인터페이스로 분리되어 호출자는 필요한 최소 기능(다음 플레이어 결정)만 의존합니다.
- 리스코프 치환/기타 원칙에 위배되는 부분은 발견되지 않았으며, 새 전략은 동일 계약을 따르므로 교체 가능성을 유지합니다.

## 3. 남은 확인 사항
- 샌드박스 제약으로 `GRADLE_USER_HOME=./.gradle-cache ./gradlew test` 실행이 `Could not determine a usable wildcard IP` 오류로 중단되었습니다. 실행 환경에서 빌드를 재시도해야 합니다.
- 확장된 턴 구조를 다루는 단위/통합 테스트(예: 전략 교체, 라운드 롤오버, 타임아웃 전환)를 추가해 회귀를 방지해야 합니다.

## 4. 참고 경로
- 전략 도입: `src/main/java/teamnova/omok/glue/game/session/interfaces/TurnAdvanceStrategy.java`, `.../services/SequentialTurnAdvanceStrategy.java`
- 서비스 조정: `src/main/java/teamnova/omok/glue/game/session/services/TurnService.java`
- 상태 반영: `src/main/java/teamnova/omok/glue/game/session/states/manage/TurnCycleContext.java`, `.../state/MoveValidatingState.java`, `.../state/TurnWaitingState.java`, `.../state/TurnFinalizingState.java`
- 메시지 인코딩: `src/main/java/teamnova/omok/glue/message/encoder/MessageEncodingUtil.java`
