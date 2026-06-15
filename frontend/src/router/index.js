import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../views/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
      { path: 'account/info', name: 'AccountInfo', component: () => import('../views/account/AccountInfo.vue'), meta: { title: '账户信息' } },
      { path: 'account/query', redirect: '/account/info' },
      { path: 'account/open', name: 'OpenAccount', component: () => import('../views/account/OpenAccount.vue'), meta: { title: '客户开户' } },
      { path: 'account/deposit', name: 'Deposit', component: () => import('../views/account/Deposit.vue'), meta: { title: '存款' } },
      { path: 'account/withdraw', name: 'Withdraw', component: () => import('../views/account/Withdraw.vue'), meta: { title: '取款' } },
      { path: 'account/transfer', name: 'Transfer', component: () => import('../views/account/Transfer.vue'), meta: { title: '转账' } },
      { path: 'account/customer', name: 'CustomerUpdate', component: () => import('../views/account/CustomerUpdate.vue'), meta: { title: '修改客户信息' } },
      { path: 'account/transactions', name: 'TransactionQuery', component: () => import('../views/account/TransactionQuery.vue'), meta: { title: '交易流水查询' } },
      { path: 'account/close', name: 'CloseAccount', component: () => import('../views/account/CloseAccount.vue'), meta: { title: '销户' } },
      { path: 'account/password', name: 'ChangePassword', component: () => import('../views/account/ChangePassword.vue'), meta: { title: '修改密码' } },
      { path: 'interest/settle', name: 'InterestSettle', component: () => import('../views/interest/InterestSettle.vue'), meta: { title: '结息管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
