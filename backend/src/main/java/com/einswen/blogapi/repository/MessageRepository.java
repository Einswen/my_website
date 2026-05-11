package com.einswen.blogapi.repository;

import com.einswen.blogapi.model.Message;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT NOT NULL,
                ip_hash TEXT NOT NULL
            )
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_messages_created_at
            ON messages(created_at DESC)
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_messages_ip_hash
            ON messages(ip_hash)
            """);
    }

    public List<Message> findAllOrderByCreatedAtDesc() {
        return jdbcTemplate.query("""
            SELECT id, name, content, created_at
            FROM messages
            ORDER BY created_at DESC
            """, this::mapMessage);
    }

    public Optional<OffsetDateTime> findLatestCreatedAtByIpHash(String ipHash) {
        List<String> createdAtRows = jdbcTemplate.query("""
            SELECT created_at
            FROM messages
            WHERE ip_hash = ?
            ORDER BY created_at DESC
            LIMIT 1
            """, (resultSet, rowNum) -> resultSet.getString("created_at"), ipHash);

        String createdAt = createdAtRows.isEmpty() ? null : createdAtRows.getFirst();

        if (createdAt == null) {
            return Optional.empty();
        }

        return Optional.of(OffsetDateTime.parse(createdAt));
    }

    public Message insert(String name, String content, String createdAt, String ipHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO messages (name, content, created_at, ip_hash)
                VALUES (?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setString(2, content);
            statement.setString(3, createdAt);
            statement.setString(4, ipHash);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException("Failed to create guestbook message.");
        }

        return jdbcTemplate.queryForObject("""
            SELECT id, name, content, created_at
            FROM messages
            WHERE id = ?
            """, this::mapMessage, key.longValue());
    }

    private Message mapMessage(ResultSet resultSet, int rowNum) throws SQLException {
        return new Message(
            resultSet.getLong("id"),
            resultSet.getString("name"),
            resultSet.getString("content"),
            resultSet.getString("created_at"),
            false
        );
    }
}
