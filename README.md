# 🌿 GreenCheck - 녹색건축물 자가진단 서비스
> **제 1회 숭실x덕성 워런톤: Hack it Your Way 해커톤 프로젝트**

## 📝 Project Overview
**GreenCheck**는 어렵게 느껴지는 녹색건축물 인증 제도(G-SEED)를 대중에게 친숙하게 알리기 위해 시작된 서비스입니다. 간단한 퀴즈 형태의 자가진단 기능과 전국 녹색건축물 분포 지도를 통해 사용자가 친환경 건축 기술을 쉽게 체감하고 탄소 배출 절감에 동참할 수 있도록 돕습니다. 

- **개발 기간:** 2025.08.16 ~ 2025.08.23
- **팀 구성:** 5인 (Back-End 2인, Front-End 2인, Design 1인)
- **주요 성과:** 🏆 해커톤 우수상 수상


## 🛠️ Tech Stacks
- **Language:** `Java 17`
- **Framework:** `Spring Boot`
- **ORM:** `Spring Data JPA`
- **Database:** `MySQL`
- **Infra:** `AWS EC2`
- **Tool:** `ERD Cloud`, `Notion`, `Git`

## 🧑‍💻 My Role
저는 본 프로젝트의 백엔드 개발 파트로서 초기 API 설계부터 퀴즈 설문 로직 구현, DB관리와 서버 배포를 맡아서 작업했습니다.

### 1. 노션 문서 백엔드 API 명세서 작성

### 2. 자가진단 퀴즈 문항 처리 및 점수 연산 로직 구현
- **피드백 로직:** 산출된 점수와 사용자의 답변 데이터를 비교 분석하여, 잘하고 있는 점(Strengths)과 개선할 점(Next Actions)을 동적으로 생성하는 로직을 구현했습니다.

### 3. DB ERD 구조 수정 및 피드백, MYSQL 관리

### 4. 클라우드 인프라 구축 및 배포
- **AWS EC2 환경 구축:** **AWS EC2**를 통해 서버를 배포하였습니다.


## 📂 Repository Structure
```bash
src/main/java/com/greencheck/
├── controller/     # REST API 컨트롤러
├── domain/         # Entity 및 Enum (SurveyMode, QuizQuestion 등)
├── dto/            # Data Transfer Object (Request/Response 설계)
├── repository/     # Spring Data JPA 인터페이스 및 Custom Query
└── service/        # 비즈니스 로직 (점수 계산 및 결과 도출 핵심 엔진)
