<template>

  <div>
    <ms-test-plan-header-bar>
      <template v-slot:info>
        <select-menu
          :data="testPlans"
          :current-data="currentPlan"
          :title="$t('test_track.plan_view.plan')"
          @dataChange="changePlan"/>
      </template>
      <template v-slot:menu>
        <el-menu active-text-color="#6d317c" :default-active="activeIndex"
                 class="el-menu-demo header-menu" mode="horizontal" @select="handleSelect">
          <el-menu-item index="functional">功能测试用例</el-menu-item>
          <el-menu-item index="api">接口测试用例</el-menu-item>
          <el-menu-item index="report">报告统计</el-menu-item>
        </el-menu>
      </template>
    </ms-test-plan-header-bar>
    <test-plan-functional v-if="activeIndex === 'functional'" :plan-id="planId"/>
    <test-plan-api v-if="activeIndex === 'api'" :plan-id="planId"/>
    <test-case-statistics-report-view :test-plan="currentPlan" v-if="activeIndex === 'report'"/>
  </div>

</template>

<script>

    import NodeTree from "../../common/NodeTree";
    import TestPlanTestCaseList from "./comonents/functional/FunctionalTestCaseList";
    import TestCaseRelevance from "./comonents/functional/TestCaseFunctionalRelevance";
    import SelectMenu from "../../common/SelectMenu";
    import MsContainer from "../../../common/components/MsContainer";
    import MsAsideContainer from "../../../common/components/MsAsideContainer";
    import MsMainContainer from "../../../common/components/MsMainContainer";
    import MsTestPlanHeaderBar from "./comonents/head/TestPlanHeaderBar";
    import TestPlanFunctional from "./comonents/functional/TestPlanFunctional";
    import TestPlanApi from "./comonents/api/TestPlanApi";
    import TestCaseStatisticsReportView from "./comonents/report/statistics/TestCaseStatisticsReportView";

    export default {
      name: "TestPlanView",
      components: {
        TestCaseStatisticsReportView,
        TestPlanApi,
        TestPlanFunctional,
        MsTestPlanHeaderBar,
        MsMainContainer,
        MsAsideContainer, MsContainer, NodeTree, TestPlanTestCaseList, TestCaseRelevance, SelectMenu},
      data() {
        return {
          testPlans: [],
          currentPlan: {},
          activeIndex: "functional"
        }
      },
      computed: {
        planId: function () {
          return this.$route.params.planId;
        }
      },
      watch: {
        '$route.params.planId'() {
          this.getTestPlans();
        }
      },
      mounted() {
        this.getTestPlans();
      },
      methods: {
        getTestPlans() {
          this.$post('/test/plan/list/all', {}, response => {
            this.testPlans = response.data;
            this.testPlans.forEach(plan => {
              if (this.planId && plan.id === this.planId) {
                this.currentPlan = plan;
              }
            });
          });
        },
        changePlan(plan) {
          this.currentPlan = plan;
          this.$router.push('/track/plan/view/' + plan.id);
        },
        handleSelect(key) {
          this.activeIndex = key;
        }
      }
    }
</script>

<style scoped>

  .select-menu {
    display: inline-block;
  }

  .ms-main-container {
    height: calc(100vh - 80px - 50px);
  }

  .ms-aside-container {
    height: calc(100vh - 80px - 51px);
    margin-top: 1px;
  }

  .header-menu.el-menu--horizontal > li {
    height: 49px;
    line-height: 50px;
    color: dimgray;
  }

</style>
