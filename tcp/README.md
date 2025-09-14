# teamnova-omok-server TCP (Java)

이 디렉터리는 향후 Java TCP 서버 구현을 위한 루트입니다. 우선 간단한 "Hello World" 시작점을 제공합니다.

## 폴더 구조
```
teamnova-omok-server/
  tcp/
    src/
      Main.java   // 진입점 (Hello World)
```

## 실행 방법 (Windows PowerShell 기준)
1) tcp 디렉터리로 이동
```
cd .\tcp
```
2) 컴파일
```
javac .\src\Main.java
```
3) 실행
```
java -cp .\src Main
```

정상 실행 시 콘솔에 다음이 출력됩니다.
```
Hello World
```

> 참고: 아직 TCP 서버는 구현되지 않았으며, 이 엔트리포인트를 기반으로 점진적으로 서버 기능을 추가할 예정입니다.


## Gradle로 빌드/테스트 (Windows PowerShell)
Gradle가 설치되어 있어야 합니다(gradle 명령 사용).

- 테스트 실행
```
# 저장소 루트에서 실행
gradle -p .\tcp test
```

- 전체 빌드
```
# 저장소 루트에서 실행
gradle -p .\tcp build
```

- 빌드 산출물로 실행
```
cd .\tcp
java -cp .\build\classes\java\main Main
```

위 명령으로 컴파일/빌드/테스트가 정상 수행됩니다. 현재 테스트는 다음을 확인합니다.
- Main이 "Hello World"를 출력하는지
- Jackson(ObjectMapper)이 Map을 JSON 문자열로 직렬화할 수 있는지


## Java 17 & Runnable JAR
- 이 프로젝트는 Gradle Java Toolchain을 통해 Java 17로 빌드됩니다.
- 빌드 산출물(JAR): `tcp/build/libs/teamnova-omok-server-tcp-1.0.0.jar`
- 실행 예시 (로컬):
```
# 저장소 루트에서
gradle -p .\tcp clean build
# 실행
java -jar .\tcp\build\libs\teamnova-omok-server-tcp-1.0.0.jar
```

### 배포 파이프라인(요약)
GitHub Actions가 다음 순서로 수행합니다.
1) JDK 17 설치
2) (존재 시) tcp/gradlew 실행권한 부여
3) Gradle로 tcp 모듈 빌드
4) 빌드된 JAR만 서버로 전송
5) 서버 접속 → pkill로 기존 프로세스 종료 후 nohup으로 재시작
