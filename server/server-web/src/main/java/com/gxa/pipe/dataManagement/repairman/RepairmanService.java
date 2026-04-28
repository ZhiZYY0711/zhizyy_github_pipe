package com.gxa.pipe.dataManagement.repairman;

import com.gxa.pipe.entity.Repairman;

import java.math.BigDecimal;
import java.util.List;

public interface RepairmanService {

    RepairmanPageQueryResponse queryRepairmen(RepairmanQueryRequest request);

    Repairman getById(Long id);

    boolean addRepairman(RepairmanAddRequest request);

    boolean updateRepairman(RepairmanUpdateRequest request);

    boolean deleteRepairman(Long id);

    boolean batchDeleteRepairmen(List<Long> ids);

    RepairmanIndicatorResponse getRepairmanIndicator(RepairmanIndicatorRequest request);

    int countTotal();

    BigDecimal calculateAverageCompletionTime();
}
