package com.recallio.group.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les groupes de l'utilisateur (ADR 0015) et ses défis (ADR 0020). En partant, il ne laisse pas un groupe sans
 * créateur : le rôle passe au membre le plus ancien, et un groupe dont il était le seul membre est supprimé. Les défis
 * qu'il a lancés et ses réponses restent : ce sont ceux du groupe.
 */
@ApplicationScoped
public class GroupPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public GroupPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "group";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("group_member", sql.rowsOf("group_member", "joined_at, group_id", userId));
        tables.put(
                "challenge",
                sql.rows("select * from challenge where created_by = :userId order by created_at, id", userId));
        tables.put("challenge_answer", sql.rowsOf("challenge_answer", "presented_at, challenge_id, position", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        // Seul membre : le groupe part, avec ses réglages, son invitation et ses défis (ON DELETE CASCADE).
        sql.execute("""
                delete from study_group g
                where exists (select 1 from group_member m where m.group_id = g.id and m.user_id = :userId)
                  and not exists (select 1 from group_member o where o.group_id = g.id and o.user_id <> :userId)
                """, userId);
        // Créateur d'un groupe qui a d'autres membres : le plus ancien d'entre eux le devient. Un groupe n'a qu'un
        // créateur à la fois (index unique) : l'utilisateur rend donc le rôle avant qu'un autre le reçoive.
        sql.execute("update group_member set role = 'MEMBER' where user_id = :userId and role = 'OWNER'", userId);
        sql.execute("""
                update group_member set role = 'OWNER'
                where (group_id, user_id) in (
                    select distinct on (o.group_id) o.group_id, o.user_id
                    from group_member o
                    where o.user_id <> :userId
                      and o.group_id in (select m.group_id from group_member m where m.user_id = :userId)
                      and not exists (
                          select 1 from group_member w where w.group_id = o.group_id and w.role = 'OWNER')
                    order by o.group_id, o.joined_at, o.user_id)
                """, userId);
        sql.deleteFrom("group_member", userId);
    }
}
