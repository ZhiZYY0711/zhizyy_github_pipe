package com.gxa.pipe.manoeuvre;

import java.util.List;

/**
 * 演习服务接口
 */
public interface ManoeuvreService {

    /**
     * 根据条件查询演习信息
     * 
     * @param request 查询请求
     * @return 演习信息列表
     */
    List<ManoeuvreQueryResponse> findManoeuvreByParams(ManoeuvreQueryRequest request);

    /**
     * 根据ID查询演习信息
     * 
     * @param id 演习ID
     * @return 演习信息列表
     */
    List<ManoeuvreQueryResponse> findManoeuvreById(Long id);

    /**
     * 添加演习信息
     * 
     * @param request 演习添加请求
     */
    void addManoeuvre(ManoeuvreAddRequest request);

    /**
     * 修改演习信息
     * 
     * @param request 演习修改请求
     */
    void updateManoeuvre(ManoeuvreUpdateRequest request);

    /**
     * 删除演习信息
     * 
     * @param id 演习ID
     */
    void removeManoeuvre(Long id);
}