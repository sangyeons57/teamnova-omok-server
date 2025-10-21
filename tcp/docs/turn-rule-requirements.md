# Turn & Rule Requirements Assessment

## 요구사항 1: 전체 턴 구조, 플레이어 순서, 활성 사용자 정보

현황  
: 게임 세션은 `TurnStore`에 턴 순서(`TurnOrder`)와 카운터(`TurnCounters`) 및 현재 인덱스를 저장합니다 (`src/main/java/teamnova/omok/glue/game/session/model/TurnStore.java:12`, `src/main/java/teamnova/omok/glue/game/session/model/vo/TurnCounters.java:6`). `TurnService`는 시작/진행 시 스냅샷을 계산하고 타이밍을 갱신하며 (`src/main/java/teamnova/omok/glue/game/session/services/TurnService.java:35`), 규칙은 `RulesContext`를 통해 상태 컨텍스트와 서비스에 접근해 해당 정보를 읽을 수 있습니다 (`src/main/java/teamnova/omok/glue/rule/RulesContext.java:66`). 실제 규칙 구현도 `turn.actionNumber()` 등을 사용 중입니다 (`src/main/java/teamnova/omok/glue/rule/rules/PerTurnAdjacentBlockerRule.java:40`). 활성 사용자 수는 참가자 뷰에서 접속 끊김 정보를 제외해 계산 가능합니다 (`src/main/java/teamnova/omok/glue/game/session/model/ParticipantsStore.java:68`).

추가 필요  
: 규칙이 반복 계산 없이 턴 관련 스냅샷과 활성 사용자 정보를 공유받도록 읽기 전용 뷰 객체(예: `RuleTurnStateView`)를 노출하면 개발 편의성이 높습니다.

주의사항  
: 상태 머신이 가진 실시간 스토어를 그대로 노출하면 동시성 문제가 생길 수 있으므로 규칙 훅이 실행되는 스레드에서만 새 뷰를 생성하고 재사용해야 합니다.

권장 방식  
: `GameSessionServices`에 `RuleTurnStateView` 팩토리를 주입해 `TurnSnapshot`, 현재/비활성 사용자 목록, 라운드 번호를 한 번 계산하고 규칙으로 전달하도록 확장합니다.

TODO  
: 1. `glue/game/session/services`에 `RuleTurnStateView`(가칭) 추가 및 단위 테스트 작성.  
  2. 기존 규칙을 해당 뷰 사용으로 리팩터링하고 개발 가이드에 예시 추가.

## 요구사항 2: 전체 턴 종료 상태(모든 플레이어 소비 후 이벤트)

현황  
: 턴 파이프라인은 `TurnFinalizingState`에서 다음 플레이어를 선택하고 결과를 큐잉하며 (`src/main/java/teamnova/omok/glue/game/session/states/state/TurnFinalizingState.java:38`), 이 진입 시점에 `TurnFinalizingSignal`이 규칙을 호출합니다 (`src/main/java/teamnova/omok/glue/game/session/states/signal/TurnFinalizingSignal.java:25`). 하지만 모든 플레이어가 한 번씩 행동한 라운드 완료 여부는 저장되지 않고, `TurnService.advanceSkippingDisconnected`가 반환한 `wrapped` 플래그도 외부에 전달되지 않습니다 (`src/main/java/teamnova/omok/glue/game/session/services/TurnService.java:47`, `src/main/java/teamnova/omok/glue/game/session/model/vo/TurnCounters.java:18`).

추가 필요  
: 라운드 종료를 알리는 별도 신호를 만들거나 `TurnCycleContext`에 `wrapped` 여부를 기록해 규칙이 전체 턴 종료와 단순 턴 종료를 구분할 수 있도록 해야 합니다. 이름은 `TurnRoundCompletedSignal` 정도가 직관적입니다.

주의사항  
: 모든 사용자가 끊겼을 때는 `advanceWithoutActive()`가 호출되어 포지션이 0으로 남으므로 (`src/main/java/teamnova/omok/glue/game/session/model/vo/TurnCounters.java:33`) 라운드 종료로 오인하지 않게 분기 처리가 필요합니다.

권장 방식  
: `TurnService.advanceSkippingDisconnected` 결과에서 `TurnAdvanceStrategy.Result.wrapped`를 `TurnCycleContext`에 저장하고, `TurnFinalizingState`가 이를 감지하면 별도 이벤트 버퍼(예: `contextService.turn().queueRoundCompletion`)를 채운 뒤 규칙에서 읽게 합니다. 동시에 `TurnRoundCompletedSignal`을 `LifecycleEventKind.ON_START`로 등록해 전체 턴 종료 전용 규칙을 구성할 수 있습니다.

TODO  
: 1. `TurnCycleContext`에 `wrapped` 필드를 추가하고 테스트로 라운드 증가 케이스를 검증.  
  2. `TurnRoundCompletedSignal` 구현과 규칙 샘플 추가, 문서화.

## 요구사항 3: 전체 턴 시작 이벤트

현황  
: 게임 시작 시에는 `ReadyResult.firstTurn`으로 첫 턴 스냅샷을 돌려주지만 (`src/main/java/teamnova/omok/glue/game/session/model/result/ReadyResult.java:11`), 이후 턴이 시작될 때는 `TurnFinalizingState`가 만든 `TurnSnapshot`을 메시지/타임아웃 스케줄링에만 사용합니다 (`src/main/java/teamnova/omok/glue/game/session/states/state/TurnFinalizingState.java:49`, `src/main/java/teamnova/omok/glue/game/session/services/events/MoveEventProcessor.java:32`). `TURN_WAITING` 진입을 감지하는 신호는 없어서 규칙이나 외부 이벤트가 턴 시작을 훅킹할 수 없습니다.

추가 필요  
: 새 턴이 열릴 때 한 번만 호출되는 신호나 콜백(예: `TurnStartSignal`)을 추가해야 합니다. `TurnCycleContext`가 큐잉한 `nextSnapshot`을 활용하면 중복을 피할 수 있습니다.

주의사항  
: `MoveValidatingState`가 유효하지 않은 입력을 받으면 `TURN_WAITING`으로 즉시 되돌아가므로 (`src/main/java/teamnova/omok/glue/game/session/states/state/MoveValidatingState.java:77`) 정상적인 턴 시작과 오류 복귀를 구분해야 합니다. `activeTurnCycle`가 비어 있고 `nextSnapshot`이 존재할 때만 신호를 발생시키는 식으로 조건을 제한해야 합니다.

권장 방식  
: `TurnFinalizingState`가 `nextSnapshot`을 큐잉한 뒤 `TurnStartSignal`을 디스패치하도록 `GameSessionStateContextService`에 작은 버퍼를 추가하고, 규칙에서는 해당 스냅샷을 사용해 턴 시작 이벤트를 처리하도록 설계합니다.

TODO  
: 1. `contextService.turn()`에 턴 시작 알림 버퍼 및 소비 API 추가.  
  2. `TurnStartSignal` 작성, 테스트 케이스에서 오류 복귀 시 신호가 발생하지 않는지 확인.

## 요구사항 4: 돌별 턴 정보와 효율적 보드 저장

현황  
: 보드는 `BoardStore`의 1차원 `byte[]`로 관리되고 (`src/main/java/teamnova/omok/glue/game/session/model/BoardStore.java:8`), `GameSession.setStone`이 값을 직접 기록합니다 (`src/main/java/teamnova/omok/glue/game/session/model/GameSession.java:258`). 스냅샷 역시 동일한 바이트 배열을 복제해 전송합니다 (`src/main/java/teamnova/omok/glue/game/session/model/messages/BoardSnapshotUpdate.java:32`). 돌이 언제 놓였는지/누가 놓았는지에 대한 별도 메타데이터는 없습니다.

추가 필요  
: 턴 기반 추적을 위해 돌 배치 메타데이터(예: `TurnPlacementStore`)를 보드와 동기화해야 합니다. 배열 길이는 동일하되 `short/int` 값으로 턴 번호나 규칙 태그를 저장하는 방식이 적합합니다.

주의사항  
: `BoardService`뿐 아니라 규칙들이 직접 `setStone`을 호출하므로 (`src/main/java/teamnova/omok/glue/rule/rules/StoneConversionRule.java`, `src/main/java/teamnova/omok/glue/rule/rules/GoCaptureRule.java`) 모든 쓰기 경로가 메타데이터도 갱신하도록 보장해야 합니다. 리셋 시 동기화 누락이 발생하지 않게 주의해야 합니다.

권장 방식  
: `GameSession`에 `TurnPlacementStore`를 추가하고, `BoardService`를 통해서만 돌 쓰기가 이뤄지도록 조정하면서 메타데이터를 함께 갱신합니다. 스냅샷 전송은 기존 바이트 배열을 유지하되, 개발용 API로 별도의 JSON/바이너리 덤프를 제공해 테스트 중 필요한 경우 참조하도록 합니다.

TODO  
: 1. `TurnPlacementStore` 구현 및 `GameSession`/`BoardService`에 통합.  
  2. 메타데이터 스냅샷/리셋 테스트 추가, 규칙 문서에 사용 예시 기재.

## 요구사항 5: 인게임 중 플레이어 턴 순서 변경

현황  
: `TurnOrder`는 불변 리스트이며 (`src/main/java/teamnova/omok/glue/game/session/model/vo/TurnOrder.java:15`), 게임 시작 이후에는 `TurnService`가 순서를 재구성하지 않습니다. `GameSession.order()` 세터는 존재하지만 (`src/main/java/teamnova/omok/glue/game/session/model/GameSession.java:282`) 런타임에서 호출할 경로가 없습니다.

추가 필요  
: 새 순서를 안전하게 적용하고 현재 인덱스를 조정하는 API가 필요합니다. `TurnService`에 `reseedOrder` 같은 메서드를 도입해 락 구간 내에서 순서·카운터·타이밍을 함께 재계산하도록 해야 합니다.

주의사항  
: 순서를 바꿀 때 현재 플레이어가 새 순서에 존재하지 않으면 예외 상황이 발생합니다. 또한 턴 타임아웃이 이미 예약돼 있다면 취소 후 재예약을 해야 하므로 `TurnTimeoutCoordinator` 연동도 고려해야 합니다 (`src/main/java/teamnova/omok/glue/game/session/services/coordinator/TurnTimeoutCoordinator.java:34`).

권장 방식  
: `TurnService.reseedOrder(store, newOrder, pivotPlayerId)`를 도입해 기준 플레이어를 유지하거나 첫 플레이어로 돌리는 옵션을 제공하고, 성공 시 새로운 `TurnSnapshot`을 반환해 타임아웃과 브로드캐스트를 즉시 갱신하도록 합니다.

TODO  
: 1. `TurnService`에 재시드 메서드 추가 및 동시성 테스트 작성.  
  2. 순서 변경 후 타임아웃 재스케줄 로직과 메시지 브로드캐스트 흐름을 검증하는 통합 테스트 추가.

## 요구사항 6: 각 돌 주변 돌/공간 계산

현황  
: 보드 서비스는 오목 판정용 직선 탐색만 제공합니다 (`src/main/java/teamnova/omok/glue/game/session/services/BoardService.java:42`). 규칙들은 인접한 칸을 직접 반복문으로 조사하고 있어 (`src/main/java/teamnova/omok/glue/rule/rules/PerTurnAdjacentBlockerRule.java:66`, `src/main/java/teamnova/omok/glue/rule/rules/GoCaptureRule.java:34`) 공통 로직이 중복됩니다.

추가 필요  
: 4·8방향 이웃, 빈칸 클러스터, 연속 돌 길이를 빠르게 구해주는 헬퍼(예: `BoardNeighborhoodService`)를 마련하면 규칙 구현이 간결해집니다.

주의사항  
: 매 턴 전체 보드를 재스캔하면 성능이 급격히 떨어질 수 있으므로, 필요 시 좌표 기반 캐시나 국소 탐색만을 제공해야 합니다. 동시에 `BoardSnapshotUpdate`로 전송하는 바이트 배열과 호환되는 좌표계를 유지해야 합니다.

권장 방식  
: `modules` 계층에 `BoardNeighborhoodService`를 추가하고, 방향 벡터와 경계 체크를 캡슐화한 뒤 규칙에서 주입받아 사용하도록 합니다. 필요 시 최근 변경 지점을 기반으로 인접 계산을 제한하는 API도 함께 설계합니다.

TODO  
: 1. `BoardNeighborhoodService` 설계 및 단위 테스트(이웃/공백 계산) 작성.  
  2. 기존 규칙을 서비스 사용으로 리팩터링하고 성능 회귀 측정을 위한 마이크로벤치마크 추가.
