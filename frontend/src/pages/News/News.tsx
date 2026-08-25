import { Newspaper, ExternalLink, ChevronLeft, ChevronRight } from 'lucide-react';
import { useNews, NEWS_CATEGORIES } from '../../hooks/useNews';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import type { NewsEntry } from '../../types';
import styles from './News.module.css';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function News() {
  const { category, selectCategory, page, setPage, newsPage, loading, error } = useNews();
  const totalPages = newsPage ? Math.max(newsPage.page.totalPages, 1) : 1;

  const handleNewsClick = (entry: NewsEntry) => {
    window.open(entry.sourceUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <>
      <div className={styles.header}>
        <div>
          <div className={styles.title}>F1 News</div>
          <div className={styles.subtitle}>Latest headlines from around the paddock</div>
        </div>
      </div>

      <div className={styles.filters}>
        {NEWS_CATEGORIES.map((item) => (
          <button
            key={item}
            className={`${styles.filterBtn} ${category === item ? styles.filterActive : ''}`}
            onClick={() => selectCategory(item)}
          >
            {item === 'ALL' ? 'All' : item.charAt(0) + item.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {loading ? (
        <LoadingState label="News" />
      ) : error ? (
        <ErrorState message={error} />
      ) : !newsPage || newsPage.content.length === 0 ? (
        <div className={styles.empty}>
          <Newspaper />
          <p>No news yet</p>
        </div>
      ) : (
        <>
          <div className={styles.list}>
            {newsPage.content.map((entry) => (
              <article key={entry.id} className={styles.card} onClick={() => handleNewsClick(entry)}>
                {entry.coverImage && (
                  <img className={styles.cover} src={entry.coverImage} alt="" loading="lazy" />
                )}
                <div className={styles.body}>
                  <div className={styles.meta}>
                    <span className={styles.category}>{entry.category}</span>
                    <span>{entry.source}</span>
                    <span>{formatDate(entry.publishedAt)}</span>
                  </div>
                  <h3 className={styles.headline}>
                    <a
                      href={entry.sourceUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={styles.headlineLink}
                      onClick={(e) => e.stopPropagation()}
                    >
                      {entry.title}
                    </a>
                  </h3>
                  {entry.summary && <p className={styles.summary}>{entry.summary}</p>}
                  <span className={styles.readMore}>
                    Read full article
                    <ExternalLink size={14} />
                  </span>
                </div>
              </article>
            ))}
          </div>

          <div className={styles.pager}>
            <button
              className={styles.pageBtn}
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              <ChevronLeft size={16} />
              Prev
            </button>
            <span className={styles.pageInfo}>
              Page {page + 1} / {totalPages}
            </span>
            <button
              className={styles.pageBtn}
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
              <ChevronRight size={16} />
            </button>
          </div>
        </>
      )}
    </>
  );
}
