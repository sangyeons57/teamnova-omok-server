<?php
class TermsRepository {
    private PDO $pdo;
    private NormalizeService $normalize;

    public function __construct(PDO $pdo, NormalizeService $normalize) {
        $this->pdo = $pdo;
        $this->normalize = $normalize;
    }

    /**
     * 사용자가 동의한 약관 목록을 반환합니다.
     * 반환 컬럼: terms_id, terms_type, version, is_required, published_at, accepted_at
     */
    public function findAcceptedByUserId($userId): array
    {
        $sql = '
            SELECT 
                t.terms_id,
                t.terms_type,
                t.version,
                t.is_required,
                t.published_at,
                uta.accepted_at
            FROM user_terms_acceptances uta
            INNER JOIN terms t ON t.terms_id = uta.terms_id
            WHERE uta.user_id = :uid
            ORDER BY t.published_at DESC, t.terms_id ASC
        ';
        $st = $this->pdo->prepare($sql);
        $st->execute(array(':uid' => $userId));
        $rows = $st->fetchAll(PDO::FETCH_ASSOC);
        return $rows ?: array();
    }

    /**
     * 사용자가 모든 "필수" 약관에 동의했는지 여부를 반환합니다.
     * 기준: terms.is_required = 1 AND published_at <= NOW()
     */
    public function allRequiredAcceptedByUserId($userId): bool
    {
        $sql = '
            SELECT COUNT(*) AS missing
            FROM terms t
            WHERE t.is_required = 1
              AND t.published_at IS NOT NULL
              AND t.published_at <= NOW()
              AND NOT EXISTS (
                    SELECT 1
                    FROM user_terms_acceptances uta
                    WHERE uta.user_id = :uid
                      AND uta.terms_id = t.terms_id
              )
        ';
        $st = $this->pdo->prepare($sql);
        $st->execute(array(':uid' => $userId));
        $row = $st->fetch(PDO::FETCH_ASSOC);
        $missing = $row ? (int)$row['missing'] : 0;
        return $missing === 0;
    }

    /**
     * terms_type 목록을 받아 각 타입의 "최신 게시된" 약관(최대 published_at)에 대해 동의를 기록합니다.
     * - 이미 동의한 약관은 건너뜁니다.
     * - 반환: ['accepted_count' => int, 'accepted_terms_ids' => int[]]
     */
    public function acceptByTypes($userId, array $types): array
    {
        // 정규화: 공통 유틸 사용(트림 + 중복 제거 + 빈값 제거)
        $types = $this->normalize->stringArrayDistinctTrimmed($types);
        if (empty($types)) {
            return array('accepted_count' => 0, 'accepted_terms_ids' => array());
        }

        // 플레이스홀더 구성 (terms_type)
        $inPh = array();
        $typeParams = array();
        foreach ($types as $i => $type) {
            $ph = ':t' . $i;
            $inPh[] = $ph;
            $typeParams[$ph] = $type;
        }
        // IN 절 문자열 사전 생성
        $inList = implode(',', $inPh);

        // 단일 쿼리: GROUP BY로 타입별 최신본 선택 + 미동의만 INSERT
        $sqlUpsert = "
            INSERT INTO user_terms_acceptances (user_id, terms_id, accepted_at)
            SELECT :uid, x.terms_id, :now
            FROM (
                SELECT t.terms_id
                FROM terms t
                JOIN (
                    SELECT terms_type, MAX(version) AS max_ver
                    FROM terms
                    WHERE terms_type IN ($inList)
                      AND published_at <= NOW()
                    GROUP BY terms_type
                ) m ON m.terms_type = t.terms_type AND m.max_ver = t.version
            ) x
            ON DUPLICATE KEY UPDATE accepted_at = VALUES(accepted_at)
        ";
        $params = array_merge(
            $typeParams,
            array(
                ':uid' => $userId,
                ':now' => (new DateTime('now', new DateTimeZone('UTC')))->format('Y-m-d H:i:s'),
            )
        );
        $st = $this->pdo->prepare($sqlUpsert);
        $st->execute($params);
        $inserted = (int)$st->rowCount();

        return array('accepted_count' => $inserted, 'accepted_terms_ids' => array());
    }
}
