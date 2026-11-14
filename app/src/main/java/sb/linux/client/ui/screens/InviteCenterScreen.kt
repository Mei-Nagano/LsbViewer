package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

/** 邀请状态、托管表单与奖励统计共享源站原生管理协议。 */
@Composable
fun InviteCenterScreen(session: Session, nav: NavHostController) {
    NativeAccountScreen(session, nav, "邀请中心", "/invite_center", onChanged = { session.refreshSession() })
}
