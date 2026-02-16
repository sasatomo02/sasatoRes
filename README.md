
# SasatoResLib

Java 21 / Spring Boot 環境での利用を想定した、**API 共通レスポンスライブラリ**です。

## 開発の背景
インターンでJavaを学ぶ中で、共通のレスポンスの形があるとフロントエンド、バックエンドどちらも助かるということを学び、セキュリティ対策の勉強もかねて開発を行いました
## 主な機能

- **自動セキュリティマスキング**: 
  - `debugMode` が `false`（デフォルト）の場合、スタックトレースや詳細なエラー情報を自動的に隠蔽し、外部への内部構造露出を防ぎます。
- **機密情報のサニタイズ**: 
  - `password`, `token`, `secret`, `apiKey` などの特定のキーワードを正規表現で検知し、自動的に `********` へ置換します。
- **一貫したレスポンスフォーマット**:
  - すべてのレスポンスに `requestId` (UUID) と `processingTimeMs` を付与し、APIの追跡性とパフォーマンスモニタリングを容易にします。

## 使い方

### 1. セキュリティ設定
環境に応じてデバッグモードを切り替えます。

```java
// 本番環境（スタックトレース等を隠蔽）
SasatoRes.setDebugMode(false);

// 開発環境（詳細なエラー解析を許可）
SasatoRes.setDebugMode(true);

```

### 2. レスポンスの生成

1行で標準化されたレスポンスを生成可能です。

```java
// 成功時
return SasatoRes.success(data);

// ページネーション付き
return SasatoRes.success(data, totalCount, limit, offset);

// エラー時（機密情報は自動でサニタイズされます）
return SasatoRes.error("ERR_001", "認証に失敗しました", exception, "token=abc-123");

```

## 品質保証

JUnit 5 を用いた単体テストを完備しており、以下の項目を検証済みです。

* [x] ページネーション情報の正確な保持
* [x] デバッグモード切り替えによる隠蔽ロジックの正常動作
* [x] 正規表現による機密情報のサニタイズ（黒塗り）の有効性

## 技術スタック

* **Language**: Java 21
* **Build Tool**: Gradle 8.5
* **Test Framework**: JUnit 5

---
