<?php
class TermsRepository {
    private $pdo;

    public function __construct($pdo) {
        $this->pdo = $pdo;
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
        $types = Normalize::stringArrayDistinctTrimmed($types);
        if (empty($types)) {
            return array('accepted_count' => 0, 'accepted_terms_ids' => array());
        }

        // 플레이스홀더 구성
        $inPh = array();
        $params = array();
        foreach ($types as $i => $type) {
            $ph = ':t' . $i;
            $inPh[] = $ph;
            $params[$ph] = $type;
        }
        $params[':uid'] = $userId;

        // 각 타입의 최신 게시본 terms_id 목록 조회
        $sqlLatest = '
            SELECT t.terms_id
            FROM terms t
            INNER JOIN (
                SELECT terms_type, MAX(published_at) AS max_pub
                FROM terms
                WHERE terms_type IN (' . implode(',', $inPh) . ')
                  AND published_at IS NOT NULL
                  AND published_at <= NOW()
                GROUP BY terms_type
            ) latest ON latest.terms_type = t.terms_type AND latest.max_pub = t.published_at
        ';
        $st = $this->pdo->prepare($sqlLatest);
        $st->execute($params);
        $validIds = array_map('intval', array_column($st->fetchAll(PDO::FETCH_ASSOC), 'terms_id'));

        if (empty($validIds)) {
            return array('accepted_count' => 0, 'accepted_terms_ids' => array());
        }

        // terms_id IN (...) 중 미동의 건만 삽입
        $idPh = array();
        $params2 = array(':uid' => $userId, ':now' => (new DateTime('now', new DateTimeZone('UTC')))->format('Y-m-d H:i:s'));
        foreach ($validIds as $i => $id) {
            $ph = ':id' . $i;
            $idPh[] = $ph;
            $params2[$ph] = $id;
        }

        $sqlInsert = '
            INSERT INTO user_terms_acceptances (user_id, terms_id, accepted_at)
            SELECT :uid, t.terms_id, :now
            FROM terms t
            WHERE t.terms_id IN (' . implode(',', $idPh) . ')
              AND NOT EXISTS (
                    SELECT 1
                    FROM user_terms_acceptances uta
                    WHERE uta.user_id = :uid
                      AND uta.terms_id = t.terms_id
              )
        ';
        $st2 = $this->pdo->prepare($sqlInsert);
        $st2->execute($params2);
        $inserted = (int)$st2->rowCount();

        return array('accepted_count' => $inserted, 'accepted_terms_ids' => $validIds);
    }
}
