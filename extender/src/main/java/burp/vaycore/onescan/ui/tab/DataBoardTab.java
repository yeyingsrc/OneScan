package burp.vaycore.onescan.ui.tab;

import burp.vaycore.common.filter.FilterRule;
import burp.vaycore.common.filter.TableFilter;
import burp.vaycore.common.filter.TableFilterPanel;
import burp.vaycore.common.helper.UIHelper;
import burp.vaycore.common.layout.HLayout;
import burp.vaycore.common.layout.VLayout;
import burp.vaycore.common.utils.IPUtils;
import burp.vaycore.common.utils.Utils;
import burp.vaycore.common.widget.HintTextField;
import burp.vaycore.onescan.bean.FpData;
import burp.vaycore.onescan.bean.TaskData;
import burp.vaycore.onescan.common.Config;
import burp.vaycore.onescan.common.DialogCallbackAdapter;
import burp.vaycore.onescan.common.L;
import burp.vaycore.onescan.common.OnFpColumnModifyListener;
import burp.vaycore.onescan.manager.FpManager;
import burp.vaycore.onescan.ui.base.BaseTab;
import burp.vaycore.onescan.ui.widget.ImportUrlWindow;
import burp.vaycore.onescan.ui.widget.TaskTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据看板
 * <p>
 * Created by vaycore on 2022-08-07.
 */
public class DataBoardTab extends BaseTab implements ImportUrlWindow.OnImportUrlListener, OnFpColumnModifyListener {

    public static final String EVENT_IMPORT_URL = "event-import-url";
    public static final String EVENT_STOP_TASK = "event-stop-task";

    private TaskTable mTaskTable;
    private JCheckBox mListenProxyMessage;
    private JCheckBox mRemoveHeader;
    private JCheckBox mReplaceHeader;
    private JCheckBox mDirScan;
    private ArrayList<FilterRule> mLastFilters;
    private HintTextField mFilterRuleText;
    private JCheckBox mPayloadProcessing;
    private ImportUrlWindow mImportUrlWindow;

    @Override
    protected void initData() {
        FpManager.addOnFpColumnModifyListener(this);
    }

    @Override
    protected void initView() {
    }

    public String getTitleName() {
        return L.get("tab_name.databoard");
    }

    public void testInit() {
        init(new JTextArea(L.get("request")), new JTextArea(L.get("response")));
        // 添加测试数据
        for (int i = 0; i < 100; i++) {
            TaskData data = new TaskData();
            data.setMethod(i % 12 == 0 ? "POST" : "GET");
            data.setHost("https://www.baidu.com");
            data.setUrl("/?s=" + i);
            data.setTitle("百度一下，你就知道");
            data.setIp(IPUtils.randomIPv4());
            data.setStatus(200);
            data.setLength(Utils.randomInt(99999));
            FpData fp = Utils.getRandomItem(FpManager.getList());
            if (fp != null) {
                List<FpData> list = new ArrayList<>();
                list.add(fp);
                data.setFingerprint(list);
            }
            data.setFrom("Proxy");
            data.setReqResp(new Object());
            getTaskTable().addTaskData(data);
        }
    }

    public void init(Component requestTextEditor, Component responseTextEditor) {
        if (requestTextEditor == null || responseTextEditor == null) {
            return;
        }
        setLayout(new VLayout(0));
        // 控制栏
        JPanel controlPanel = new JPanel(new HLayout(5, true));
        controlPanel.setBorder(new EmptyBorder(0, 0, 0, 5));
        controlPanel.setFocusable(false);
        add(controlPanel);
        // 代理监听开关
        mListenProxyMessage = newJCheckBox(controlPanel, L.get("listen_proxy_message"), Config.KEY_ENABLE_LISTEN_PROXY);
        // 请求头移除开关
        mRemoveHeader = newJCheckBox(controlPanel, L.get("remove_header"), Config.KEY_ENABLE_REMOVE_HEADER);
        // 请求头替换开关
        mReplaceHeader = newJCheckBox(controlPanel, L.get("replace_header"), Config.KEY_ENABLE_REPLACE_HEADER);
        // 递归扫描开关
        mDirScan = newJCheckBox(controlPanel, L.get("dir_scan"), Config.KEY_ENABLE_DIR_SCAN);
        // 启用Payload Processing
        mPayloadProcessing = newJCheckBox(controlPanel, L.get("payload_processing"), Config.KEY_ENABLE_PAYLOAD_PROCESSING);
        // 导入Url
        JButton importUrlBtn = new JButton(L.get("import_url"));
        importUrlBtn.setToolTipText(L.get("import_url"));
        importUrlBtn.addActionListener((e) -> importUrl());
        controlPanel.add(importUrlBtn);
        // 停止按钮
        JButton stopBtn = new JButton(L.get("stop"));
        stopBtn.setToolTipText(L.get("stop_all_task"));
        stopBtn.addActionListener((e) -> stopTask());
        controlPanel.add(stopBtn);
        // 清空历史记录按钮
        JButton clearBtn = new JButton(L.get("clear_record"));
        clearBtn.setToolTipText(L.get("clear_history"));
        clearBtn.addActionListener((e) -> clearHistory());
        controlPanel.add(clearBtn);
        // 操作菜单按钮
        JButton actionsBtn = new JButton(L.get("actions"));
        actionsBtn.setToolTipText(L.get("actions_menu"));
        actionsBtn.addActionListener((e) -> {
            JButton btn = (JButton) e.getSource();
            if (mTaskTable != null) {
                mTaskTable.showPopupMenu(btn, 0, btn.getHeight());
            }
        });
        controlPanel.add(actionsBtn);
        // 过滤设置
        controlPanel.add(new JPanel(), "1w");
        mFilterRuleText = new HintTextField();
        mFilterRuleText.setEditable(false);
        mFilterRuleText.setHintText(L.get("no_filter_rules"));
        controlPanel.add(mFilterRuleText, "2w");
        JButton filterBtn = new JButton(L.get("filter"));
        filterBtn.setToolTipText(L.get("filter_data"));
        filterBtn.addActionListener(e -> showSetupFilterDialog());
        controlPanel.add(filterBtn, "65px");
        // 主面板
        JSplitPane mainSplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPanel.setResizeWeight(0.55D);
        mainSplitPanel.setDividerSize(3);
        // 请求列表
        mTaskTable = new TaskTable();
        JScrollPane scrollPane = new JScrollPane(mTaskTable);
        scrollPane.setPreferredSize(new Dimension(scrollPane.getWidth(), 0));
        // 请求和响应面板
        JSplitPane dataSplitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        dataSplitPanel.setResizeWeight(0.5D);
        dataSplitPanel.setDividerSize(3);
        dataSplitPanel.add(requestTextEditor, JSplitPane.LEFT);
        dataSplitPanel.add(responseTextEditor, JSplitPane.RIGHT);
        // 添加子面板控件
        mainSplitPanel.add(scrollPane, JSplitPane.LEFT);
        mainSplitPanel.add(dataSplitPanel, JSplitPane.RIGHT);
        // 将布局进行展示
        add(mainSplitPanel, "100%");
        // 加载过滤规则
        loadFilterRules();
    }

    /**
     * 从配置文件中加载过滤规则
     */
    private void loadFilterRules() {
        ArrayList<FilterRule> rules = Config.getDataboardFilterRules();
        if (rules == null) {
            return;
        }
        // 借助 TableFilterPanel 组件转换配置
        TableFilterPanel panel = new TableFilterPanel(TaskTable.getColumnNames(), rules);
        ArrayList<TableFilter<AbstractTableModel>> filters = panel.exportTableFilters();
        String rulesText = panel.exportRulesText();
        mTaskTable.setRowFilter(filters);
        mFilterRuleText.setText(rulesText);
        mLastFilters = rules;
    }

    private JCheckBox newJCheckBox(JPanel panel, String text, String configKey) {
        JCheckBox checkBox = new JCheckBox(text, Config.getBoolean(configKey));
        checkBox.setFocusable(false);
        checkBox.setMargin(new Insets(5, 5, 5, 5));
        panel.add(checkBox);
        checkBox.addActionListener(e -> {
            boolean configSelected = Config.getBoolean(configKey);
            boolean selected = checkBox.isSelected();
            if (selected == configSelected) {
                return;
            }
            // 保存配置
            Config.put(configKey, String.valueOf(selected));
        });
        return checkBox;
    }

    /**
     * 显示导入 URL 窗口
     */
    private void importUrl() {
        if (mImportUrlWindow == null) {
            mImportUrlWindow = new ImportUrlWindow();
            mImportUrlWindow.setOnImportUrlListener(this);
        }
        mImportUrlWindow.showWindow();
    }

    /**
     * 关闭导入 URL 窗口
     */
    public void closeImportUrlWindow() {
        if (mImportUrlWindow != null) {
            mImportUrlWindow.closeWindow();
        }
    }

    @Override
    public void onImportUrl(List<String> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        sendTabEvent(EVENT_IMPORT_URL, data);
        UIHelper.showTipsDialog(L.get("import_url_success"), mImportUrlWindow);
    }

    @Override
    public void onFpColumnModify() {
        if (mTaskTable != null) {
            mTaskTable.refreshColumns();
        }
    }

    /**
     * 停止扫描任务
     */
    private void stopTask() {
        // 停止任务前，优先将代理监听开关关闭
        mListenProxyMessage.setSelected(false);
        // 发送事件消息
        sendTabEvent(EVENT_STOP_TASK);
        // 通知停止添加任务数据
        if (mTaskTable != null) {
            mTaskTable.stopAddTaskData();
        }
    }

    /**
     * 清空历史记录
     */
    private void clearHistory() {
        if (mTaskTable == null) {
            return;
        }
        mTaskTable.clearAll();
    }

    /**
     * 获取任务表格组件
     */
    public TaskTable getTaskTable() {
        return mTaskTable;
    }

    /**
     * 是否开启监听代理请求开关
     */
    public boolean hasListenProxyMessage() {
        return mListenProxyMessage != null && mListenProxyMessage.isSelected();
    }

    /**
     * 是否开启移除请求头开关
     */
    public boolean hasRemoveHeader() {
        return mRemoveHeader != null && mRemoveHeader.isSelected();
    }

    /**
     * 是否开启替换请求头开关
     */
    public boolean hasReplaceHeader() {
        return mReplaceHeader != null && mReplaceHeader.isSelected();
    }

    /**
     * 是否开启目录扫描开关
     */
    public boolean hasDirScan() {
        return mDirScan != null && mDirScan.isSelected();
    }

    /**
     * 是否开启请求包处理开关
     */
    public boolean hasPayloadProcessing() {
        return mPayloadProcessing != null && mPayloadProcessing.isSelected();
    }

    /**
     * 设置过滤对话框
     */
    private void showSetupFilterDialog() {
        TableFilterPanel panel = new TableFilterPanel(TaskTable.getColumnNames(), mLastFilters);
        panel.showDialog(new DialogCallbackAdapter() {

            @Override
            public void onConfirm(ArrayList<FilterRule> filterRules, ArrayList<TableFilter<AbstractTableModel>> filters, String rulesText) {
                mTaskTable.setRowFilter(filters);
                mFilterRuleText.setText(rulesText);
                mLastFilters = filterRules;
                Config.put(Config.KEY_DATABOARD_FILTER_RULES, filterRules);
            }

            @Override
            public void onReset() {
                mTaskTable.setRowFilter(null);
                mFilterRuleText.setText("");
                mLastFilters = null;
                Config.put(Config.KEY_DATABOARD_FILTER_RULES, new ArrayList<>());
            }
        });
    }
}
