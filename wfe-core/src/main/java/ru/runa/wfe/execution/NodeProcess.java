package ru.runa.wfe.execution;

import com.google.common.base.Objects;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class NodeProcess<P extends Process, T extends Token<P, T>> {

    protected String nodeId;
    protected Integer index;  // TODO why = 0 for subprocess?
    protected Date createDate;

    @Transient
    public abstract boolean isArchive();

    @Transient
    public abstract Long getId();
    protected abstract void setId(Long id);

    @Transient
    public abstract P getProcess();
    public abstract void setProcess(P process);

    @Transient
    public abstract P getSubProcess();
    public abstract void setSubProcess(P subProcess);

    @Transient
    public abstract P getRootProcess();
    public abstract void setRootProcess(P subProcess);

    @Transient
    public abstract T getParentToken();
    public abstract void setParentToken(T parentToken);


    @Column(name = "PARENT_NODE_ID", length = 1024)
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Column(name = "SUBPROCESS_INDEX")
    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Column(name = "CREATE_DATE", nullable = false)
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", getId()).add("nodeId", nodeId).add("process", getProcess()).add("subProcess", getSubProcess())
                .toString();
    }
}
