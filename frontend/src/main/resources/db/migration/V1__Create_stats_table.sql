CREATE TABLE rushing_stats
(
    id                                INT PRIMARY KEY AUTO_INCREMENT,
    player                            TEXT          NOT NULL,
    team                              TEXT          NOT NULL,
    position                          TEXT          NOT NULL,
    rushing_attempts                  INT           NOT NULL,
    rushing_attempts_per_game         DECIMAL(5, 2) NOT NULL,
    rushing_yards                     INT           NOT NULL,
    rushing_average_yards_per_attempt DECIMAL(5, 2) NOT NULL,
    rushing_yards_per_game            DECIMAL(5, 2) NOT NULL,
    total_rushing_touchdowns          INT           NOT NULL,
    longest_rush                      INT           NOT NULL,
    touchdown_occurred                BOOLEAN       NOT NULL,
    rushing_first_downs               INT           NOT NULL,
    rushing_first_downs_percentage    DECIMAL(5, 2) NOT NULL,
    rushing_20_plus_yards             INT           NOT NULL,
    rushing_40_plus_yards             INT           NOT NULL,
    rushing_fumbles                   INT           NOT NULL
)
;

CREATE TABLE exports
(
    uuid BINARY(16) PRIMARY KEY,
    user_uuid BINARY(16) NOT NULL,
    created_on TIMESTAMP NOT NULL,
    file_key VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    original_request TEXT NOT NULL
)
;
