package com.intuit.app.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This is the base class of all the Nodes.
 */


@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseNode {

    /**
     * This is the nodeId made from the unix timestamp coming from the front end.
     * This cannot be passed as null front frontend.
     */
    private String nodeId;
    /**
     * deleted will have value true if this node has been deleted on the front end.
     */
    private boolean deleted;

    /**
     * deleted will have value true if this node has been trashed on the front end.
     */
    private boolean trashed;

    /**
     * This will store the type of Node.
     * @see NodeType
     */
    private NodeType nodeType ;

    /**
     * This stores the User who created this node.
     */
    private User createdBy;

    /**
     * This stores the User who updated this node.
     */
    private User lastModifiedBy;

    /**
     * This stores all the timestamps related to this node. All the timestamps are coming from the front end.
     */
    private Timestamps timestamps;
    /**
     * This is the parent id of the NoteNode it is associated with.
     * It is default to "root" for NoteNode types NoteNode and ListNode.
     * NodeType ListItem and Attachment will have parent id of either NoteNode or ListNode.
     *
     */
    private String parentId = "root";

    private Integer baseVersion;

    private String title;

    private List<Long> attachmentList;
    private boolean pinned;
    private boolean isArchived;
    private List<User> collaborators;
    private List<Label> labels;

    private String text;
    private boolean isChecked;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isTrashed() {
        return trashed;
    }

    public void setTrashed(boolean trashed) {
        this.trashed = trashed;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(User lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Timestamps getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(Timestamps timestamps) {
        this.timestamps = timestamps;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getBaseVersion() {
        return baseVersion;
    }

    public void setBaseVersion(Integer baseVersion) {
        this.baseVersion = baseVersion;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Long> getAttachmentList() {
        return attachmentList;
    }

    public void setAttachmentList(List<Long> attachmentList) {
        this.attachmentList = attachmentList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public List<User> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<User> collaborators) {
        this.collaborators = collaborators;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
