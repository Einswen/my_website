package com.einswen.blogapi.repository;

import com.einswen.blogapi.model.PetChatMessage;
import com.einswen.blogapi.model.PetStateRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PetRepository {

    private final JdbcTemplate jdbcTemplate;

    public PetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS pet_state (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                name TEXT NOT NULL,
                color TEXT NOT NULL,
                outfit_id TEXT NOT NULL,
                satiety INTEGER NOT NULL,
                last_satiety_update TEXT NOT NULL,
                last_fed_at TEXT,
                last_interacted_at TEXT,
                updated_at TEXT NOT NULL
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS pet_chat_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_pet_chat_logs_created_at
            ON pet_chat_logs(created_at DESC)
            """);
    }

    public void ensureDefaultPetState(OffsetDateTime now) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pet_state WHERE id = 1", Integer.class);

        if (count != null && count > 0) {
            return;
        }

        String timestamp = now.toString();
        jdbcTemplate.update("""
            INSERT INTO pet_state (
                id, name, color, outfit_id, satiety, last_satiety_update, last_fed_at, last_interacted_at, updated_at
            )
            VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            "我",
            "pink-white",
            "berry-bow",
            82,
            timestamp,
            timestamp,
            timestamp,
            timestamp
        );
    }

    public PetStateRecord getPetState() {
        return jdbcTemplate.queryForObject("""
            SELECT id, name, color, outfit_id, satiety, last_satiety_update, last_fed_at, last_interacted_at, updated_at
            FROM pet_state
            WHERE id = 1
            """, this::mapPetState);
    }

    public void updatePetState(PetStateRecord record) {
        jdbcTemplate.update("""
            UPDATE pet_state
            SET name = ?, color = ?, outfit_id = ?, satiety = ?, last_satiety_update = ?, last_fed_at = ?, last_interacted_at = ?, updated_at = ?
            WHERE id = 1
            """,
            record.name(),
            record.color(),
            record.outfitId(),
            record.satiety(),
            record.lastSatietyUpdate(),
            record.lastFedAt(),
            record.lastInteractedAt(),
            record.updatedAt()
        );
    }

    public void appendChatMessage(String role, String content, OffsetDateTime createdAt) {
        jdbcTemplate.update("""
            INSERT INTO pet_chat_logs (role, content, created_at)
            VALUES (?, ?, ?)
            """, role, content, createdAt.toString());
    }

    public List<PetChatMessage> findRecentChatMessages(int limit) {
        return jdbcTemplate.query("""
            SELECT role, content
            FROM pet_chat_logs
            ORDER BY created_at DESC
            LIMIT ?
            """, (resultSet, rowNum) -> new PetChatMessage(
            resultSet.getString("role"),
            resultSet.getString("content")
        ), limit).reversed();
    }

    private PetStateRecord mapPetState(ResultSet resultSet, int rowNum) throws SQLException {
        return new PetStateRecord(
            resultSet.getLong("id"),
            resultSet.getString("name"),
            resultSet.getString("color"),
            resultSet.getString("outfit_id"),
            resultSet.getInt("satiety"),
            resultSet.getString("last_satiety_update"),
            resultSet.getString("last_fed_at"),
            resultSet.getString("last_interacted_at"),
            resultSet.getString("updated_at")
        );
    }
}
