package com.intuit.app.dao.notes;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.intuit.app.dao.label.ILabelDao;
import com.intuit.app.dao.mappers.NoteMapper;
import com.intuit.app.models.BaseNode;
import com.intuit.app.models.Label;
import com.intuit.app.models.NodeType;
import com.intuit.app.service.notes.NotesService;
import com.intuit.app.utils.DBUtill;
import com.intuit.app.web.change.NodesChangeRequest;

@Transactional
@Repository
public class NotesDao implements INotesDao {

    @Autowired
    ILabelDao labelDao;

    private static final String ROOT = "root";
    private static final String EXISTS_SQL = "SELECT count(*) FROM NODE WHERE node_id = ?";
    private static final String INSERT_NODE_SQL = "INSERT INTO NODE (" +
            "node_id," +
            "parent_node_id," +
            "node_type," +
            "title," +
            "text," +
            "isChecked," +
            "isPinned," +
            "isArchived," +
            "baseVersion," +
            "created_date," +
            "updated_date) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_NODE_SQL = "UPDATE NODE SET " +
            "node_type = ?," +
            "title = ?," +
            "text = ?," +
            "isChecked = ?," +
            "isPinned = ?," +
            "isArchived = ?," +
            "baseVersion = ?," +
            "created_date = ?," +
            "updated_date = ? " +
            "WHERE node_id = ?";

    private static final String DELETE_NODE_SQL = "DELETE FROM NODE WHERE node_id = ?";

    private static final String DELETE_ROOT_NODE = "DELETE FROM NODE WHERE node_id IN ( " +
            "  SELECT * FROM (         " +
            "   SELECT n2.node_id FROM NODE n " +
            "   LEFT JOIN NODE n2 ON " +
            "   n2.parent_node_id = n.node_id             " +
            "   WHERE n.node_id = ? " +
            "            UNION " +
            "            SELECT n.node_id FROM NODE n " +
            "            WHERE n.node_id = ? " +
            "   ) AS p " +
            " )";

    private static final String DELETE_LABEL_SQL = "DELETE FROM NODE_LABEL WHERE node_id = :node_id and label_id in (:deletedLabels)";

    private static final String INSERT_LABEL_SQL = "INSERT INTO NODE_LABEL (node_id, label_id) " +
            "SELECT * FROM ( SELECT :node_id, :label_id ) As tmp " +
            "WHERE NOT EXISTS ( " +
            "SELECT node_id,label_id FROM NODE_LABEL WHERE node_id = :node_id and label_id = :label_id" +
            ") LIMIT 1";

    private static final Logger logger = LoggerFactory.getLogger(NotesService.class);


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<List<BaseNode>> getNotes(){
        List<List<BaseNode>> result = new ArrayList<>();
        String sql = "SELECT node_id, title, isPinned, isArchived FROM NODE";
        RowMapper<BaseNode> rowMapper = new NoteMapper();
        final List<BaseNode> baseNodes = this.jdbcTemplate.query(sql, rowMapper);

        return result;
    }


    @Override
    @Transactional
    public void insertOrUpdate(NodesChangeRequest nodesChangeRequest) {
        logger.debug("NotesDao::insertOrUpdate : requestId is {}, nodes in this request are {}",nodesChangeRequest.getRequestId(),nodesChangeRequest.getNodeList().size());
        for(BaseNode node:nodesChangeRequest.getNodeList()){
            final String nodeId = node.getNodeId();
            if(nodeExists(nodeId)){
                if(node.isDeleted()){
                    deleteNode(node);
                }else
                    updateNode(node);
            }else{
                insertNode(node);
            }
        }
    }

    /**
     * @apiNote This method expects only two types of the node i.e LIST_ITEM and ROOT. If its LIST_ITEM, it will be simply deleted
     *          from the database. If it's ROOT node, then all the LIST_ITEM nodes and BLOB nodes associated with it will be
     *          deleted with this ROOT node. In case of ROOT node, deleteNodeAndAllChildNodes method  will be called.
     * @param node
     *        This is either the LIST_ITEM node or the ROOT node. If this is ROOT node, then all its children will be deleted.
     */
    private void deleteNode(final BaseNode node) {
        logger.debug("NotesDao::deleteNode Deleting Node with id {}",node.getNodeId());
        if(node.getParentId().equals(ROOT)){
            deleteNodeAndAllChildNodes(node);
        }else {
            final int delete = jdbcTemplate.update(DELETE_NODE_SQL, node.getNodeId());
            logger.debug("NotesDao::deleteNode : delete is {}",delete);
        }
    }

    /**
     *
     * @param node
     *        ROOT node to be deleted. It will also delete all the children nodes (LIST_ITEM and BLOB) too.
     */
    private void deleteNodeAndAllChildNodes(BaseNode node) {
        final int delete = jdbcTemplate.update(DELETE_ROOT_NODE, node.getNodeId(),node.getNodeId());
        logger.debug("NotesDao::deleteNodeAndAllChildNodes : delete is {}",delete);
    }

    private void insertNode(BaseNode node) {
        if(node.getNodeType().equals(NodeType.BLOB)){
            insertAttachment();
        }else{
            final int update = jdbcTemplate.update(INSERT_NODE_SQL,
                    node.getNodeId(),
                    node.getParentId(),
                    node.getNodeType().getValue(),
                    node.getTitle(),
                    node.getText(),
                    DBUtill.getString(node.isChecked()),
                    DBUtill.getString(node.isPinned()),
                    DBUtill.getString(node.isArchived()),
                    node.getBaseVersion(),
                    DBUtill.convertToJavaSqlTimeStamp(node.getTimestamps().getCreated()),
                    DBUtill.convertToJavaSqlTimeStamp(node.getTimestamps().getUpdated()));
            logger.debug("NotesDao::insertNode : update is {}",update);

        }
    }

    private void insertAttachment() {
    }

    private void updateNode(BaseNode node) {

            if(node.getParentId().equals(ROOT)){
                if (node.isTrashed()) {
                    updateNodeAndChildren(node);
                }
                updateLabels(node);
            }
            else {
                final int update = jdbcTemplate.update(UPDATE_NODE_SQL,
                        node.getNodeType().getValue(),
                        node.getTitle(),
                        node.getText(),
                        DBUtill.getString(node.isChecked()),
                        DBUtill.getString(node.isPinned()),
                        DBUtill.getString(node.isArchived()),
                        node.getBaseVersion(),
                        DBUtill.convertToJavaSqlTimeStamp(node.getTimestamps().getCreated()),
                        DBUtill.convertToJavaSqlTimeStamp(node.getTimestamps().getUpdated()),
                        node.getNodeId());
                logger.debug("NotesDao::updateNode : update is {}", update);
            }

    }

    private void updateLabels(BaseNode node) {
        List<Long> selectedLabels = new ArrayList<>();
        List<Long> deletedLabels = new ArrayList<>();
        for(Label label:node.getLabels()){
            if(label.isDeleted()){
                deletedLabels.add(label.getLabelId());
            }else if(label.isSelected()){
                selectedLabels.add(label.getLabelId());
            }
        }
        if(deletedLabels.size() > 0){
            MapSqlParameterSource namedParameters = new MapSqlParameterSource();
            namedParameters.addValue("deletedLabels", deletedLabels);
            namedParameters.addValue("node_id",node.getNodeId());
            int delete = namedParameterJdbcTemplate.update(DELETE_LABEL_SQL, namedParameters);
            logger.debug("NotesDao::updateLabels : delete is {}", delete);
        }
        if(selectedLabels.size() > 0){
            for(Long labelId:selectedLabels) {
                MapSqlParameterSource namedParameters = new MapSqlParameterSource();
                namedParameters.addValue("label_id", labelId);
                namedParameters.addValue("node_id", node.getNodeId());
                int insert = namedParameterJdbcTemplate.update(INSERT_LABEL_SQL, namedParameters);
                logger.debug("NotesDao::updateLabels : insert is {}", insert);
            }
        }
    }

    private void updateNodeAndChildren(BaseNode node) {
    }

    private boolean nodeExists(String id) {
        String nodeId = jdbcTemplate.queryForObject(EXISTS_SQL,String.class,id);
        return !nodeId.equals("0");
    }

}
