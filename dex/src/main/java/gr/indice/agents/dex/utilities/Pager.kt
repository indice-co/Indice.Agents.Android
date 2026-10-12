package gr.indice.agents.dex.utilities

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import gr.indice.agents.dex.BuildConfig
import gr.indice.agents.dex.models.ResultSet

import kotlin.coroutines.cancellation.CancellationException

class Pager<T: Any>(private val pageSize: Int = 10,
                    private val prefetchedDistance: Int = 3,
                    var hasLocalFilter: Boolean,
                    private val getter: suspend (page: Int, size: Int) -> ResultSet<T>
) {

    private var pagingSource: PagingSource<Int, T>? = null

    private val pager by lazy {
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = prefetchedDistance,
                initialLoadSize = pageSize
            ),
            initialKey = 1,
            pagingSourceFactory = ::dataPagingSource
        )
    }
    fun invalidate() {
        pagingSource?.invalidate()
    }

    fun getPagerFlow() = pager.flow

    private fun dataPagingSource(): PagingSource<Int, T> {
        return object : PagingSource<Int, T>() {
            override fun getRefreshKey(state: PagingState<Int, T>): Int? {
                return null
            }

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
                return try {

                    val currentPage = (params.key ?: 1).coerceAtLeast(1)
                    val result = getter(currentPage, params.loadSize)
                    val items = result.items ?: emptyList()
                    val canFetchMore = (result.count ?: 0) > (currentPage * params.loadSize)

                    LoadResult.Page(
                        data = items,
                        prevKey = if (currentPage == 1) null else currentPage - 1,
                        nextKey = if (canFetchMore && hasLocalFilter.not()) currentPage + 1 else null
                    )
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                    if (e is CancellationException)
                        return LoadResult.Error(e)
                    return LoadResult.Error(e)
                }
            }

        }
    }
}