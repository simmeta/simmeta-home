# SimMeta Android WebView App

SimMeta 웹사이트([https://simmet-home.netlify.app](https://simmet-home.netlify.app))를 풀스크린으로 표시하는 고성능 안드로이드 웹뷰 앱 프로젝트입니다.

---

## 📱 앱 주요 사양 및 특징

- **사이트 URL**: `https://simmet-home.netlify.app`
- **화면 모드**: 다크 테마 일체형 전체화면 (Edge-to-Edge Fullscreen)
- **제공 기능**:
  - **뒤로 가기(Back) 제스처 지원**: 웹 내 페이지 뒤로 가기 및 연속 2회 입력 시 앱 종료 방지 알림
  - **당겨서 새로고침(Swipe to Refresh)**: 최상단에서 아래로 당겨서 즉시 리로드
  - **파일 / 사진 업로드 완벽 지원**: `<input type="file">` 클릭 시 안드로이드 갤러리/카메라/파일 선택기 연동
  - **웹 알림창(Alert/Confirm) 지원**: 모던 다크 테마 다이얼로그 팝업
  - **오프라인/네트워크 오류 화면**: 인터넷 연결이 끊겼을 때 감성적인 다크 에러 UI 및 '다시 시도' 버튼 제공
  - **다운로드 매니저 연동**: 웹 내 다운로드 링크 터치 시 안드로이드 시스템 다운로드 연동
  - **외부 링크/앱 연동**: 전화(`tel:`), 이메일(`mailto:`), 문자(`sms:`) 및 외부 도메인 자동 분기 처리
  - **앱 아이콘**: SimMeta 브랜드 심볼(3D 테크 레이어 스택 + 네온 글로우) 고해상도 Adaptive Icon 적용 (mdpi ~ xxxhdpi, Play Store 512px)

---

## 🚀 GitHub Actions로 APK 빌드하는 방법 (Android Studio 불필요)

내 컴퓨터에 Android Studio나 Java, Android SDK가 전혀 설치되어 있지 않아도 **GitHub Actions를 통해 클라우드에서 2~3분 만에 APK를 자동 빌드**할 수 있습니다.

### 1단계: GitHub 저장소에 푸시(Push)
이 프로젝트 폴더 전체를 GitHub 저장소에 올립니다.
```bash
git add .
git commit -m "feat: Add Android WebView app and GitHub Actions workflow"
git push origin main
```

### 2단계: 자동 빌드 확인
1. GitHub 저장소 웹페이지로 이동합니다.
2. 상단 메뉴의 **[Actions]** 탭을 클릭합니다.
3. **"Build Android APK (SimMeta)"** 워크플로우가 자동으로 실행 중인 것을 볼 수 있습니다. (약 2~3분 소요)
   - *팁: 원할 때마다 [Actions] -> [Build Android APK] -> [Run workflow] 버튼을 눌러 언제든 수동으로 새 APK를 빌드할 수도 있습니다.*

### 3단계: APK 다운로드 및 스마트폰 설치
1. 빌드가 완료되면 초록색 체크 표시(✅)가 뜹니다. 해당 빌드를 클릭합니다.
2. 페이지 하단의 **Artifacts (아티팩트)** 섹션에서 **`SimMeta-Android-APKs`** 항목을 클릭하여 ZIP 파일을 다운로드합니다.
3. 압축을 풀면 두 가지 APK가 포함되어 있습니다:
   - **`SimMeta-Debug.apk`**: 개발 및 테스트용 (스마트폰에 바로 설치 가능)
   - **`SimMeta-Release.apk`**: 최적화 릴리즈용 (서명 완료되어 바로 설치 가능)
4. APK 파일을 갤럭시/안드로이드 스마트폰으로 전송(카카오톡 나에게 보내기, 구글 드라이브 등)한 뒤 탭하여 설치합니다.
   - *설치 시 "출처를 알 수 없는 앱 설치" 허용 팝업이 뜨면 '허용'을 선택하시면 됩니다.*

---

## 🛠 프로젝트 구조

```
android/
├── .github/workflows/
│   └── build-apk.yml               # 단독 레포로 업로드할 때 사용되는 워크플로우
├── app/
│   ├── src/main/
│   │   ├── java/com/simmeta/app/
│   │   │   └── MainActivity.java   # 웹뷰 및 풀스크린 핵심 로직
│   │   ├── res/
│   │   │   ├── drawable/           # 벡터 로고, 프로그레스바, 버튼 리소스
│   │   │   ├── layout/             # activity_main.xml (웹뷰 및 오프라인 UI)
│   │   │   ├── mipmap-*/           # SimMeta 고해상도 앱 아이콘 (전 밀도 지원)
│   │   │   └── values/             # 색상, 문자열, 다크 테마
│   │   ├── AndroidManifest.xml     # 앱 권한 및 액티비티 설정
│   │   └── playstore-icon.png      # 구글 플레이스토어 등록용 512x512 아이콘
│   └── build.gradle                # 앱 모듈 빌드 설정
├── gradle/wrapper/                 # Gradle Wrapper (gradle-wrapper.jar 포함)
├── build.gradle                    # 프로젝트 빌드 설정
├── settings.gradle                 # 프로젝트 설정
├── gradlew / gradlew.bat           # 빌드 스크립트
└── README.md
```

---

## ⚙️ 설정 변경 가이드

- **접속 웹사이트 URL 변경**:  
  `app/src/main/java/com/simmeta/app/MainActivity.java` 파일 상단의 `TARGET_URL` 상수 값을 원하는 URL로 변경하면 됩니다.
  ```java
  public static final String TARGET_URL = "https://simmet-home.netlify.app";
  ```
- **앱 이름 변경**:  
  `app/src/main/res/values/strings.xml` 파일의 `app_name`을 변경합니다.
  ```xml
  <string name="app_name">SimMeta</string>
  ```

