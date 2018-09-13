package ru.runa.wfe.audit.dao;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import ru.runa.wfe.audit.ProcessLog;
import ru.runa.wfe.commons.dao.CommonDAO;
import ru.runa.wfe.commons.dao.ConstantDao;
import ru.runa.wfe.definition.dao.IProcessDefinitionLoader;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.execution.Token;

@Component
public class AggregatedProcessLogAwareDao extends CommonDAO implements ProcessLogAwareDao {

    @Autowired
    private IProcessDefinitionLoader processDefinitionLoader;
    @Autowired
    private ConstantDao constantDao;
    @Autowired
    private ProcessLogDao processLogDao;

    @Override
    public void addLog(ProcessLog processLog, Process process, Token token) {
        UpdateAggregatedLogOperation op = new UpdateAggregatedLogOperation(sessionFactory, queryFactory, processDefinitionLoader, process, token);
        processLog.processBy(op);
    }
}
